package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"regexp"
	"sort"
	"strings"
	"text/template"

	"golang.org/x/sys/unix"
)

func ensure(envVar string) bool {
	_, found := os.LookupEnv(envVar)
	return found
}

func path(filePath string, operation string) bool {
	switch operation {

	case "readable":
		return unix.Access(filePath, unix.R_OK) == nil
	case "executable":
		info, err := os.Stat(filePath)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error checking executable status of file %s: %s", filePath, err)
			return false
		}
		return info.Mode()&0111 != 0 //check whether file is executable by anyone, use 0100 to check for execution rights for owner
	case "existence":
		if _, err := os.Stat(filePath); os.IsNotExist(err) {
			return false
		}
		return true
	case "writable":
		return unix.Access(filePath, unix.W_OK) == nil
	default:
		fmt.Fprintf(os.Stderr, "Unknown operation %s", operation)
	}
	return false
}

func renderTemplate(templateFilePath string) bool {
	templateFile, err := os.Open(templateFilePath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error opening file at path %s: %s", templateFilePath, err)
		return false
	}
	bytes, err := io.ReadAll(templateFile)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error reading from file at path %s: %s", templateFilePath, err)
		return false
	}
	funcs := template.FuncMap{
		"getEnv":             getEnvOrDefault,
		"splitToMapDefaults": splitToMapDefaults,
	}
	t, err := template.New("tmpl").Funcs(funcs).Parse(string(bytes))
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error  %s: %s", templateFilePath, err)
		return false
	}
	return buildTemplate(os.Stdout, *t)
}

func buildTemplate(writer io.Writer, template template.Template) bool {
	err := template.Execute(writer, GetEnvironment())
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error building template file : %s", err)
		return false
	}
	return true
}

func renderConfig(writer io.Writer, configSpec ConfigSpec) bool {
	return writeConfig(writer, buildProperties(configSpec, GetEnvironment()))
}

// ConvertKey Converts an environment variable name to a property-name according to the following rules:
// - a single underscore (_) is replaced with a .
// - a double underscore (__) is replaced with a single underscore
// - a triple underscore (___) is replaced with a dash
// Moreover, the whole string is converted to lower-case.
// The behavior of sequences of four or more underscores is undefined.
func ConvertKey(key string) string {
	re := regexp.MustCompile("[^_]_[^_]")
	singleReplaced := re.ReplaceAllStringFunc(key, replaceUnderscores)
	singleTripleReplaced := strings.ReplaceAll(singleReplaced, "___", "-")
	return strings.ToLower(strings.ReplaceAll(singleTripleReplaced, "__", "_"))
}

// replaceUnderscores replaces every underscore '_' by a dot '.'
func replaceUnderscores(s string) string {
	return strings.ReplaceAll(s, "_", ".")
}

type ConfigSpec struct {
	Prefixes          map[string]bool   `json:"prefixes"`
	Excludes          []string          `json:"excludes"`
	Renamed           map[string]string `json:"renamed"`
	Defaults          map[string]string `json:"defaults"`
	ExcludeWithPrefix string            `json:"excludeWithPrefix"`
}

// Contains returns true if slice contains element, and false otherwise.
func Contains(slice []string, element string) bool {
	for _, v := range slice {
		if v == element {
			return true
		}
	}
	return false
}

// ListToMap splits each and entry of the kvList argument at '=' into a key/value pair and returns a map of all the k/v pair thus obtained.
func ListToMap(kvList []string) map[string]string {
	m := make(map[string]string)
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
			if !Contains(spec.Excludes, envKey) && !(len(spec.ExcludeWithPrefix) > 0 && strings.HasPrefix(envKey, spec.ExcludeWithPrefix)) {
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

func writeConfig(writer io.Writer, config map[string]string) bool {
	// Go randomizes iterations over map by design. We sort properties by name to ease debugging:
	sortedNames := make([]string, 0, len(config))
	for name := range config {
		sortedNames = append(sortedNames, name)
	}
	sort.Strings(sortedNames)
	for _, n := range sortedNames {
		_, err := fmt.Fprintf(writer, "%s=%s\n", n, config[n])
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error printing configs: %s", err)
			return false
		}
	}
	return true
}

func loadConfigSpec(path string) (ConfigSpec, bool) {
	var spec ConfigSpec
	jsonFile, err := os.Open(path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error opening json file %s : %s", path, err)
		return spec, false
	}
	bytes, err := io.ReadAll(jsonFile)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error reading from json file %s : %s", path, err)
		return spec, false
	}

	errParse := json.Unmarshal(bytes, &spec)
	if errParse != nil {
		fmt.Fprintf(os.Stderr, "Error parsing json file %s : %s", path, errParse)
	}
	return spec, true
}

func invokeJavaCommand(className string, jvmOpts string, args []string) bool {
	classPath := getEnvOrDefault("UB_CLASSPATH", "/usr/share/java/cp-base-lite/*")

	opts := []string{}
	if jvmOpts != "" {
		opts = append(opts, jvmOpts)
	}
	opts = append(opts, "-cp", classPath, className)
	cmd := exec.Command("java", append(opts[:], args...)...)

	if err := cmd.Run(); err != nil {
		if exitError, ok := err.(*exec.ExitError); ok {
			return exitError.ExitCode() == 0
		}
		return false
	}
	return true
}

func getEnvOrDefault(envVar string, defaultValue string) string {
	v, found := os.LookupEnv(envVar)
	if !found {
		return defaultValue
	}
	return v
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
		opts = append(opts, "s", security)
	}
	jvmOpts := os.Getenv("KAFKA_OPTS")
	return invokeJavaCommand("io.confluent.admin.utils.cli.KafkaReadyCommand", jvmOpts, opts)
}

func checkAndPrintUsage(numArguments int, message string) {
	if len(os.Args) != numArguments {
		fmt.Fprintf(os.Stderr, "Usage '%s %s %s' \n", os.Args[0], os.Args[1], message)
		os.Exit(1)
	}
}

func main() {
	success := false
	if len(os.Args) < 2 {
		fmt.Fprintf(os.Stderr, "Usage '%s <subcommand> ...'", os.Args[0])
		os.Exit(1)
	}
	switch os.Args[1] {
	case "ensure":
		checkAndPrintUsage(3, "<env-variable>")
		success = ensure(os.Args[2])
	case "path":
		checkAndPrintUsage(4, "<path-to-file> <operation>")
		success = path(os.Args[2], os.Args[3])
	case "render-template":
		// render a template (used for log4j properties)
		checkAndPrintUsage(3, "<path-to-template>")
		success = renderTemplate(os.Args[2])
	case "render-properties":
		checkAndPrintUsage(3, "<path-to-config-spec>")
		configSpec, isSuccess := loadConfigSpec(os.Args[2])
		if isSuccess {
			success = renderConfig(os.Stdout, configSpec)
		}
	case "kafka-ready":
		//first positional argument: number brokers
		//second positional argument: timeout in seconds
		kafkaReadyCmd := flag.NewFlagSet("kafka-ready", flag.ExitOnError)
		kafkaReadyBootstrap := kafkaReadyCmd.String("b", "", "Bootstrap broker list")
		kafkaReadyZooKeeper := kafkaReadyCmd.String("z", "", "ZooKeeper connect string")
		kafkaReadyConfig := kafkaReadyCmd.String("c", "", "Path to config properties")
		kafkaReadySecurity := kafkaReadyCmd.String("s", "", "Security protocol")

		kafkaReadyCmd.Parse(os.Args[2:])
		if kafkaReadyCmd.NArg() != 2 {
			fmt.Fprintln(os.Stderr, "Missing positional argument", kafkaReadyCmd.Args())
		} else {
			success = checkKafkaReady(kafkaReadyCmd.Arg(0), kafkaReadyCmd.Arg(1), *kafkaReadyBootstrap, *kafkaReadyZooKeeper, *kafkaReadyConfig, *kafkaReadySecurity)
		}
	default:
		fmt.Fprintln(os.Stderr, "Unknown subcommand "+os.Args[1])
	}

	if !success {
		os.Exit(1)
	}
}
