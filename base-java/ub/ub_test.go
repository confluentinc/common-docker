package main

import (
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"reflect"
	"testing"
	"time"
)

func assertEqual(a string, b string, t *testing.T) {
	if a != b {
		t.Error(a + " != " + b)
	}
}

func Test_ensure(t *testing.T) {
	type args struct {
		envVar string
	}
	err := os.Setenv("ENV_VAR", "value")
	if err != nil {
		t.Fatal("Unable to set ENV_VAR for the test")
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "should exist",
			args: args{
				envVar: "ENV_VAR",
			},
			want: true,
		},
		{
			name: "should not exist",
			args: args{
				envVar: "RANDOM_ENV_VAR",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := ensure(tt.args.envVar); got != tt.want {
				t.Errorf("ensure() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_path(t *testing.T) {
	type args struct {
		filePath  string
		operation string
	}
	const (
		sampleFile       = "testResources/sampleFile"
		sampleFile2      = "testResources/sampleFile2"
		fileDoesNotExist = "testResources/sampleFile3"
	)
	err := os.Chmod(sampleFile, 0777)
	if err != nil {
		t.Error("Unable to set permissions for the file")
	}
	err = os.Chmod(sampleFile2, 0000)
	if err != nil {
		t.Error("Unable to set permissions for the file")
	}
	tests := []struct {
		name    string
		args    args
		want    bool
		wantErr bool
	}{
		{
			name: "file readable",
			args: args{filePath: sampleFile,
				operation: "readable"},
			want:    true,
			wantErr: false,
		},
		{
			name: "file writable",
			args: args{filePath: sampleFile,
				operation: "writable"},
			want:    true,
			wantErr: false,
		},
		{
			name: "file executable",
			args: args{filePath: sampleFile,
				operation: "executable"},
			want:    true,
			wantErr: false,
		},
		{
			name: "file existence",
			args: args{filePath: sampleFile,
				operation: "existence"},
			want:    true,
			wantErr: false,
		},
		{
			name: "file not readable",
			args: args{filePath: sampleFile2,
				operation: "readable"},
			want:    false,
			wantErr: true,
		},
		{
			name: "file not writable",
			args: args{filePath: sampleFile2,
				operation: "writable"},
			want:    false,
			wantErr: true,
		},
		{
			name: "file not executable",
			args: args{filePath: sampleFile2,
				operation: "executable"},
			want:    false,
			wantErr: false,
		},
		{
			name: "file does not exist",
			args: args{filePath: fileDoesNotExist,
				operation: "existence"},
			want:    false,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := path(tt.args.filePath, tt.args.operation)
			if (err != nil) != tt.wantErr {
				t.Errorf("path() error = %v, wantErr %v", err, tt.wantErr)
			}
			if got != tt.want {
				t.Errorf("path() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_renderTemplate(t *testing.T) {
	type args struct {
		templateFilePath string
	}
	const (
		fileExistsAndRenderable = "testResources/sampleLog4j.template"
		fileDoesNotExist        = "testResources/RandomFileName"
	)
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name:    "render template success",
			args:    args{templateFilePath: fileExistsAndRenderable},
			wantErr: false,
		},
		{
			name:    "render template failure ",
			args:    args{templateFilePath: fileDoesNotExist},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := renderTemplate(tt.args.templateFilePath); (err != nil) != tt.wantErr {
				t.Errorf("renderTemplate() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
func Test_convertKey(t *testing.T) {
	type args struct {
		key string
	}
	tests := []struct {
		name       string
		args       args
		wantString string
	}{
		{
			name:       "Capitals",
			args:       args{key: "KEY"},
			wantString: "key",
		},
		{
			name:       "Capitals with underscore",
			args:       args{key: "KEY_FOO"},
			wantString: "key.foo",
		},
		{
			name:       "Capitals with double underscore",
			args:       args{key: "KEY__UNDERSCORE"},
			wantString: "key_underscore",
		},
		{
			name:       "Capitals with double and single underscore",
			args:       args{key: "KEY_WITH__UNDERSCORE_AND__MORE"},
			wantString: "key.with_underscore.and_more",
		},
		{
			name:       "Capitals with triple underscore",
			args:       args{key: "KEY___DASH"},
			wantString: "key-dash",
		},
		{
			name:       "capitals with double,triple and single underscore",
			args:       args{key: "KEY_WITH___DASH_AND___MORE__UNDERSCORE"},
			wantString: "key.with-dash.and-more_underscore",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if result := ConvertKey(tt.args.key); result != tt.wantString {
				t.Errorf("ConvertKey() result = %v, wantStr %v", result, tt.wantString)
			}
		})
	}
}

func Test_buildProperties(t *testing.T) {
	type args struct {
		spec        ConfigSpec
		environment map[string]string
	}
	tests := []struct {
		name string
		args args
		want map[string]string
	}{
		{
			name: "only defaults",
			args: args{
				spec: ConfigSpec{
					Defaults: map[string]string{
						"default.property.key": "default.property.value",
						"bootstrap.servers":    "unknown",
					},
				},
				environment: map[string]string{
					"PATH":                          "thePath",
					"KAFKA_BOOTSTRAP_SERVERS":       "localhost:9092",
					"CONFLUENT_METRICS":             "metricsValue",
					"KAFKA_IGNORED":                 "ignored",
					"KAFKA_EXCLUDE_PREFIX_PROPERTY": "ignored",
				},
			},
			want: map[string]string{"bootstrap.servers": "unknown", "default.property.key": "default.property.value"},
		},
		{
			name: "server properties",
			args: args{
				spec: ConfigSpec{
					Prefixes: map[string]bool{"KAFKA": false, "CONFLUENT": true},
					Excludes: []string{"KAFKA_IGNORED"},
					Renamed:  map[string]string{},
					Defaults: map[string]string{
						"default.property.key": "default.property.value",
						"bootstrap.servers":    "unknown",
					},
					ExcludeWithPrefix: "KAFKA_EXCLUDE_PREFIX_",
				},
				environment: map[string]string{
					"PATH":                          "thePath",
					"KAFKA_BOOTSTRAP_SERVERS":       "localhost:9092",
					"CONFLUENT_METRICS":             "metricsValue",
					"KAFKA_IGNORED":                 "ignored",
					"KAFKA_EXCLUDE_PREFIX_PROPERTY": "ignored",
				},
			},
			want: map[string]string{"bootstrap.servers": "localhost:9092", "confluent.metrics": "metricsValue", "default.property.key": "default.property.value"},
		},
		{
			name: "kafka properties",
			args: args{
				spec: ConfigSpec{
					Prefixes: map[string]bool{"KAFKA": false, "CONFLUENT": true},
					Excludes: []string{"KAFKA_IGNORED"},
					Renamed:  map[string]string{},
					Defaults: map[string]string{
						"default.property.key": "default.property.value",
						"bootstrap.servers":    "unknown",
					},
					ExcludeWithPrefix: "KAFKA_EXCLUDE_PREFIX_",
				},
				environment: map[string]string{
					"KAFKA_FOO":     "foo",
					"KAFKA_FOO_BAR": "bar",
					"KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG": `org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";`,
					"KAFKA_IGNORED":                   "ignored",
					"KAFKA_WITH__UNDERSCORE":          "with underscore",
					"KAFKA_WITH__UNDERSCORE_AND_MORE": "with underscore and more",
					"KAFKA_WITH___DASH":               "with dash",
					"KAFKA_WITH___DASH_AND_MORE":      "with dash and more",
				},
			},
			want: map[string]string{
				"bootstrap.servers":    "unknown",
				"default.property.key": "default.property.value",
				"foo":                  "foo",
				"foo.bar":              "bar",
				"listener.name.broker.plain.sasl.jaas.config": `org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";`,
				"with-dash":                "with dash",
				"with-dash.and.more":       "with dash and more",
				"with_underscore":          "with underscore",
				"with_underscore.and.more": "with underscore and more",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			for k, v := range tt.args.environment {
				t.Setenv(k, v)
			}

			if got := buildProperties(tt.args.spec, GetEnvironment()); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("buildProperties() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_splitToMapDefaults(t *testing.T) {
	type args struct {
		separator     string
		defaultValues string
		value         string
	}
	tests := []struct {
		name string
		args args
		want map[string]string
	}{
		{
			name: "split to default",
			args: args{
				separator:     ",",
				defaultValues: "kafka=INFO,kafka.producer.async.DefaultEventHandler=DEBUG,state.change.logger=TRACE",
				value:         "kafka.producer.async.DefaultEventHandler=ERROR,kafka.request.logger=WARN",
			},
			want: map[string]string{"kafka": "INFO", "kafka.producer.async.DefaultEventHandler": "ERROR", "kafka.request.logger": "WARN", "state.change.logger": "TRACE"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := splitToMapDefaults(tt.args.separator, tt.args.defaultValues, tt.args.value); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("splitToMapDefaults() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_waitForServer(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))
	defer mockServer.Close()
	port := mockServer.Listener.Addr().(*net.TCPAddr).Port

	type args struct {
		host    string
		port    int
		timeout time.Duration
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "invalid server address",
			args: args{
				host:    "localhost",
				port:    port + 1,
				timeout: time.Duration(5) * time.Second,
			},
			want: false,
		},
		{
			name: "valid server address",
			args: args{
				host:    "localhost",
				port:    port,
				timeout: time.Duration(5) * time.Second,
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := waitForServer(tt.args.host, tt.args.port, tt.args.timeout); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("waitForServer() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_waitForHttp(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/names" {
			w.WriteHeader(http.StatusOK)
		} else {
			http.NotFound(w, r)
		}
	}))
	defer mockServer.Close()

	serverURL := mockServer.URL

	type args struct {
		URL     string
		timeout time.Duration
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "valid server address, valid url",
			args: args{
				URL:     serverURL + "/names",
				timeout: time.Duration(5) * time.Second,
			},
			wantErr: false,
		},
		{
			name: "valid server address, invalid url",
			args: args{
				URL:     serverURL,
				timeout: time.Duration(5) * time.Second,
			},
			wantErr: true,
		},
		{
			name: "invalid server address",
			args: args{
				URL:     "http://invalidAddress:50111/names",
				timeout: time.Duration(5) * time.Second,
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := waitForHttp(tt.args.URL, tt.args.timeout); (err != nil) != tt.wantErr {
				t.Errorf("waitForHttp() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestEnvToProps(t *testing.T) {
	tests := []struct {
		name       string
		envVars    map[string]string
		envPrefix  string
		propPrefix string
		exclude    []string
		expected   map[string]string
	}{
		{
			name: "Basic conversion with prefix",
			envVars: map[string]string{
				"APP_FOO_BAR": "value1",
				"APP_BAZ":     "value2",
				"OTHER_VAR":   "ignored",
			},
			envPrefix:  "APP_",
			propPrefix: "app.",
			exclude:    []string{},
			expected: map[string]string{
				"app.foo.bar": "value1",
				"app.baz":     "value2",
			},
		},
		{
			name: "With exclusions",
			envVars: map[string]string{
				"APP_FOO_BAR": "value1",
				"APP_SECRET":  "hidden",
				"APP_BAZ":     "value2",
			},
			envPrefix:  "APP_",
			propPrefix: "app.",
			exclude:    []string{"APP_SECRET"},
			expected: map[string]string{
				"app.foo.bar": "value1",
				"app.baz":     "value2",
			},
		},
		{
			name: "With special underscore conversions",
			envVars: map[string]string{
				"APP_SINGLE_UNDERSCORE":   "dot",
				"APP_DOUBLE__UNDERSCORE":  "single_underscore",
				"APP_TRIPLE___UNDERSCORE": "dash",
			},
			envPrefix:  "APP_",
			propPrefix: "app.meta.",
			exclude:    []string{},
			expected: map[string]string{
				"app.meta.single.underscore": "dot",
				"app.meta.double_underscore": "single_underscore",
				"app.meta.triple-underscore": "dash",
			},
		},
		{
			name: "Empty result",
			envVars: map[string]string{
				"OTHER_VAR": "ignored",
			},
			envPrefix:  "APP_",
			propPrefix: "app.",
			exclude:    []string{},
			expected:   map[string]string{},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			for k, v := range tt.envVars {
				os.Setenv(k, v)
			}
			defer func() {
				for k, _ := range tt.envVars {
					os.Unsetenv(k)
				}
			}()
			result := envToProps(tt.envPrefix, tt.propPrefix, tt.exclude)
			if !reflect.DeepEqual(result, tt.expected) {
				t.Errorf("envToProps() = %v, want %v", result, tt.expected)
			}

		})
	}
}
