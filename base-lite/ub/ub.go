package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"regexp"
	"sort"
	"strings"
	"text/template"

	pt "path"

	"github.com/spf13/cobra"
	"golang.org/x/exp/slices"
	"golang.org/x/sys/unix"
)

type ConfigSpec struct {
	Prefixes          map[string]bool   `json:"prefixes"`
	Excludes          []string          `json:"excludes"`
	Renamed           map[string]string `json:"renamed"`
	Defaults          map[string]string `json:"defaults"`
	ExcludeWithPrefix string            `json:"excludeWithPrefix"`
}

var re = regexp.MustCompile("[^_]_[^_]")

var ensureCmd = &cobra.Command{
	Use:   "ensure <environment-variable>",
	Short: "checks if environment variable is set or not",
	Args:  cobra.ExactArgs(1),
	Run:   runEnsureCmd,
}

var pathCmd = &cobra.Command{
	Use:   "path <path-to-file> <operation>",
	Short: "checks if an operation is permitted on a file",
	Args:  cobra.ExactArgs(2),
	Run:   runPathCmd,
}

var renderTemplateCmd = &cobra.Command{
	Use:   "render-template <path-to-template>",
	Short: "renders template to stdout",
	Args:  cobra.ExactArgs(1),
	Run:   runRenderTemplateCmd,
}

var renderPropertiesCmd = &cobra.Command{
	Use:   "render-properties <path-to-config-spec>",
	Short: "creates and renders properties to stdout using the json config spec.",
	Args:  cobra.ExactArgs(1),
	Run:   runRenderPropertiesCmd,
}

var (
	bootstrapServers string
	configFile      string
	zookeeperConnect string
	security string
	kafkaReadyCmd   = &cobra.Command{
		Use:   "kafka-ready <min-no-of-brokers> <timeout-in-secs>",
		Short: "checks is kafka broker are up using bootstrap servers and config file",
		Args:  cobra.ExactArgs(2),
		Run:   runKafkaReadyCmd,
	}
)

func ensure(envVar string) bool {
	_, found := os.LookupEnv(envVar)
	return found
}

func path(filePath string, operation string) (bool, error) {
	switch operation {

	case "readable":
		err := unix.Access(filePath, unix.R_OK)
		if err != nil {
			return false, err
		}
		return true, nil
	case "executable":
		info, err := os.Stat(filePath)
		if err != nil {
			err = fmt.Errorf("error checking executable status of file %s: %q", filePath, err)
			return false, err
		}
		return info.Mode()&0111 != 0, nil //check whether file is executable by anyone, use 0100 to check for execution rights for owner
	case "existence":
		if _, err := os.Stat(filePath); os.IsNotExist(err) {
			return false, nil
		}
		return true, nil
	case "writable":
		err := unix.Access(filePath, unix.W_OK)
		if err != nil {
			return false, err
		}
		return true, nil
	default:
		err := fmt.Errorf("unknown operation %s", operation)
		return false, err
	}
}

func renderTemplate(templateFilePath string) error {
	funcs := template.FuncMap{
		"getEnv":             getEnvOrDefault,
		"splitToMapDefaults": splitToMapDefaults,
	}
	t, err := template.New(pt.Base(templateFilePath)).Funcs(funcs).ParseFiles(templateFilePath)
	if err != nil {
		err = fmt.Errorf("error  %s: %q", templateFilePath, err)
		return err
	}
	return buildTemplate(os.Stdout, *t)
}

func buildTemplate(writer io.Writer, template template.Template) error {
	err := template.Execute(writer, GetEnvironment())
	if err != nil {
		err = fmt.Errorf("error building template file : %q", err)
		return err
	}
	return nil
}

func renderConfig(writer io.Writer, configSpec ConfigSpec) error {
	return writeConfig(writer, buildProperties(configSpec, GetEnvironment()))
}

// ConvertKey Converts an environment variable name to a property-name according to the following rules:
// - a single underscore (_) is replaced with a .
// - a double underscore (__) is replaced with a single underscore
// - a triple underscore (___) is replaced with a dash
// Moreover, the whole string is converted to lower-case.
// The behavior of sequences of four or more underscores is undefined.
func ConvertKey(key string) string {
	singleReplaced := re.ReplaceAllStringFunc(key, replaceUnderscores)
	singleTripleReplaced := strings.ReplaceAll(singleReplaced, "___", "-")
	return strings.ToLower(strings.ReplaceAll(singleTripleReplaced, "__", "_"))
}

// replaceUnderscores replaces every underscore '_' by a dot '.'
func replaceUnderscores(s string) string {
	return strings.ReplaceAll(s, "_", ".")
}

// ListToMap splits each and entry of the kvList argument at '=' into a key/value pair and returns a map of all the k/v pair thus obtained.
// this method will only consider values in the list formatted as key=value
func ListToMap(kvList []string) map[string]string {
	m := make(map[string]string, len(kvList))
	for _, l := range kvList {
		parts := strings.Split(l, "=")
		if len(parts) == 2 {
			m[parts[0]] = parts[1]
		}
	}
	return m
}

func splitToMapDefaults(separator string, defaultValues string, value string) map[string]string {
	values := KvStringToMap(defaultValues, separator)
	for k, v := range KvStringToMap(value, separator) {
		values[k] = v
	}
	return values
}

func KvStringToMap(kvString string, sep string) map[string]string {
	return ListToMap(strings.Split(kvString, sep))
}

// GetEnvironment returns the current environment as a map.
func GetEnvironment() map[string]string {
	return ListToMap(os.Environ())
}

// buildProperties creates a map suitable to be output as Java properties from a ConfigSpec and a map representing an environment.
func buildProperties(spec ConfigSpec, environment map[string]string) map[string]string {
	config := make(map[string]string)
	for key, value := range spec.Defaults {
		config[key] = value
	}

	for envKey, envValue := range environment {
		if newKey, found := spec.Renamed[envKey]; found {
			config[newKey] = envValue
		} else {
			if !slices.Contains(spec.Excludes, envKey) && !(len(spec.ExcludeWithPrefix) > 0 && strings.HasPrefix(envKey, spec.ExcludeWithPrefix)) {
				for prefix, keep := range spec.Prefixes {
					if strings.HasPrefix(envKey, prefix) {
						var effectiveKey string
						if keep {
							effectiveKey = envKey
						} else {
							effectiveKey = envKey[len(prefix)+1:]
						}
						config[ConvertKey(effectiveKey)] = envValue
					}
				}
			}
		}
	}
	return config
}

func writeConfig(writer io.Writer, config map[string]string) error {
	// Go randomizes iterations over map by design. We sort properties by name to ease debugging:
	sortedNames := make([]string, 0, len(config))
	for name := range config {
		sortedNames = append(sortedNames, name)
	}
	sort.Strings(sortedNames)
	for _, n := range sortedNames {
		_, err := fmt.Fprintf(writer, "%s=%s\n", n, config[n])
		if err != nil {
			err = fmt.Errorf("error printing configs: %q", err)
			return err
		}
	}
	return nil
}

func loadConfigSpec(path string) (ConfigSpec, error) {
	var spec ConfigSpec
	bytes, err := os.ReadFile(path)
	if err != nil {
		err = fmt.Errorf("error reading from json file %s : %q", path, err)
		return spec, err
	}

	errParse := json.Unmarshal(bytes, &spec)
	if errParse != nil {
		err = fmt.Errorf("error parsing json file %s : %q", path, errParse)
		return spec, err
	}
	return spec, nil
}

func invokeJavaCommand(className string, jvmOpts string, args []string) bool {
	classPath := getEnvOrDefault("UB_CLASSPATH", "/usr/share/java/cp-base-lite/*")

	opts := []string{}
	if jvmOpts != "" {
		opts = append(opts, jvmOpts)
	}
	opts = append(opts, "-cp", classPath, className)
	cmd := exec.Command("java", append(opts[:], args...)...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	if err := cmd.Run(); err != nil {
		var exitError *exec.ExitError
		if errors.As(err, &exitError) {
			return exitError.ExitCode() == 0
		}
		return false
	}
	return true
}

func getEnvOrDefault(envVar string, defaultValue string) string {
	val := os.Getenv(envVar)
	if len(val) == 0 {
		return defaultValue
	}
	return val
}

func checkKafkaReady(minNumBroker string, timeout string, bootstrapServers string, zookeeperConnect string, configFile string, security string) bool {

	opts := []string{minNumBroker, timeout + "000"}
	if bootstrapServers != "" {
		opts = append(opts, "-b", bootstrapServers)
	}
	if zookeeperConnect != "" {
		opts = append(opts, "-z", zookeeperConnect)
	}
	if configFile != "" {
		opts = append(opts, "-c", configFile)
	}
	if security != "" {
		opts = append(opts, "-s", security)
	}
	jvmOpts := os.Getenv("KAFKA_OPTS")
	return invokeJavaCommand("io.confluent.admin.utils.cli.KafkaReadyCommand", jvmOpts, opts)
}

func runEnsureCmd(_ *cobra.Command, args []string) {
	success := ensure(args[0])
	if !success {
		fmt.Fprintf(os.Stderr, "environment variable %s is not set", args[0])
		os.Exit(1)
	}
}

func runPathCmd(_ *cobra.Command, args []string) {
	success, err := path(args[0], args[1])
	if err != nil {
		fmt.Fprintf(os.Stderr, "error in checking operation %q on file %q: %q", args[1], args[0], err)
		os.Exit(1)
	}
	if !success {
		fmt.Fprintf(os.Stderr, "operation %q on file %q is unsuccessful", args[1], args[0])
		os.Exit(1)
	}
}

func runRenderTemplateCmd(_ *cobra.Command, args []string) {
	err := renderTemplate(args[0])
	if err != nil {
		fmt.Fprintf(os.Stderr, "error in rendering template %s: %q", args[0], err)
		os.Exit(1)
	}
}

func runRenderPropertiesCmd(_ *cobra.Command, args []string) {
	configSpec, err := loadConfigSpec(args[0])
	if err != nil {
		fmt.Fprintf(os.Stderr, "error in loading config from file %s: %q", args[0], err)
		os.Exit(1)
	}
	err = renderConfig(os.Stdout, configSpec)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error in building properties from file %s: %q", args[0], err)
		os.Exit(1)
	}
}

func runKafkaReadyCmd(_ *cobra.Command, args []string) {
	success := checkKafkaReady(args[0], args[1], bootstrapServers, zookeeperConnect, configFile, security)
	if !success {
		fmt.Fprintf(os.Stderr, "kafka-ready check failed")
		os.Exit(1)
	}
}

func main() {
	rootCmd := &cobra.Command{
		Use:   "ub",
		Short: "utility commands for cp docker images",
		Run:   func(cmd *cobra.Command, args []string) {},
	}

	kafkaReadyCmd.PersistentFlags().StringVarP(&bootstrapServers, "bootstrap-servers", "b", "", "comma-separated list of kafka brokers")
	kafkaReadyCmd.PersistentFlags().StringVarP(&configFile, "config", "c", "", "path to the config file")
	kafkaReadyCmd.PersistentFlags().StringVarP(&zookeeperConnect, "zookeeper-connect", "z", "", "path to the config file")
	kafkaReadyCmd.PersistentFlags().StringVarP(&security, "security", "s", "", "path to the config file")

	rootCmd.AddCommand(pathCmd)
	rootCmd.AddCommand(ensureCmd)
	rootCmd.AddCommand(renderTemplateCmd)
	rootCmd.AddCommand(renderPropertiesCmd)
	rootCmd.AddCommand(kafkaReadyCmd)

	if err := rootCmd.Execute(); err != nil {
		fmt.Fprintf(os.Stderr, "error in executing the command: %q", err)
		os.Exit(1)
	}
}
