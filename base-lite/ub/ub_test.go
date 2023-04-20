package main

import (
	"fmt"
	"os"
	"testing"
)

func assertEqual(a string, b string, t *testing.T) {
	if a != b {
		t.Error(a + " != " + b)
	}
}

func TestConvertKey(t *testing.T) {
	assertEqual(ConvertKey("KEY"), "key", t)
	assertEqual(ConvertKey("KEY_FOO"), "key.foo", t)
	assertEqual(ConvertKey("KEY__UNDERSCORE"), "key_underscore", t)
	assertEqual(ConvertKey("KEY_WITH__UNDERSCORE_AND__MORE"), "key.with_underscore.and_more", t)
	assertEqual(ConvertKey("KEY___DASH"), "key-dash", t)
	assertEqual(ConvertKey("KEY_WITH___DASH_AND___MORE__UNDERSCORE"), "key.with-dash.and-more_underscore", t)
}

func TestBuildProperties(t *testing.T) {
	var testEnv = map[string]string{
		"PATH":                          "thePath",
		"KAFKA_BOOTSTRAP_SERVERS":       "localhost:9092",
		"CONFLUENT_METRICS":             "metricsValue",
		"KAFKA_IGNORED":                 "ignored",
		"KAFKA_EXCLUDE_PREFIX_PROPERTY": "ignored",
	}

	var onlyDefaultsCS = ConfigSpec{
		Prefixes: map[string]bool{},
		Excludes: []string{},
		Renamed:  map[string]string{},
		Defaults: map[string]string{
			"default.property.key": "default.property.value",
			"bootstrap.servers":    "unknown",
		},
	}

	var onlyDefaults = buildProperties(onlyDefaultsCS, testEnv)
	fmt.Println(onlyDefaults)
	if len(onlyDefaults) != 2 {
		t.Error("Failed to parse defaults.")
	}
	if onlyDefaults["default.property.key"] != "default.property.value" {
		t.Error("default.property.key not parsed correctly")
	}

	var serverCS = ConfigSpec{
		Prefixes: map[string]bool{"KAFKA": false, "CONFLUENT": true},
		Excludes: []string{"KAFKA_IGNORED"},
		Renamed:  map[string]string{},
		Defaults: map[string]string{
			"default.property.key": "default.property.value",
			"bootstrap.servers":    "unknown",
		},
		ExcludeWithPrefix: "KAFKA_EXCLUDE_PREFIX_",
	}
	var serverProps = buildProperties(serverCS, testEnv)
	if len(serverProps) != 3 {
		t.Error("Server props size != 3", serverProps)
	}
	if serverProps["bootstrap.servers"] != "localhost:9092" {
		t.Error("Dropped prefixed not parsed correctly")
	}
	if serverProps["confluent.metrics"] != "metricsValue" {
		t.Error("Kept prefix not parsed correctly")
	}

	var kafkaEnv = map[string]string{
		"KAFKA_FOO":                       "foo",
		"KAFKA_FOO_BAR":                   "bar",
		"KAFKA_IGNORED":                   "ignored",
		"KAFKA_WITH__UNDERSCORE":          "with underscore",
		"KAFKA_WITH__UNDERSCORE_AND_MORE": "with underscore and more",
		"KAFKA_WITH___DASH":               "with dash",
		"KAFKA_WITH___DASH_AND_MORE":      "with dash and more",
	}

	var kafkaProperties = buildProperties(serverCS, kafkaEnv)

	if len(kafkaProperties) != 8 {
		t.Error("Wrong number of properties")
	}
	assertEqual(kafkaProperties["foo"], "foo", t)
	assertEqual(kafkaProperties["foo.bar"], "bar", t)
	assertEqual(kafkaProperties["with_underscore"], "with underscore", t)
	assertEqual(kafkaProperties["with_underscore.and.more"], "with underscore and more", t)
	assertEqual(kafkaProperties["with-dash"], "with dash", t)
	assertEqual(kafkaProperties["with-dash.and.more"], "with dash and more", t)
}

func TestEnsure(t *testing.T) {
	err := os.Setenv("ENV_VAR", "value")
	if err != nil {
		t.Error("Unable to set ENV_VAR for the test")
	}
	if !ensure("ENV_VAR") {
		t.Error("ENV_VAR set but returned false")
	}
	if ensure("RANDOM_ENV_VAR") {
		t.Error("RANDOM_ENV_VAR not set but returned true")
	}
}

/*
func TestPath(t *testing.T) {
	sampleFile := "testResources/sampleFile"
	err := os.Chmod(sampleFile, 0664)
	if err != nil {
		t.Error("Unable to set permissions for the file")
	}
	if !path(sampleFile, "readable") {
		t.Error("File is readable but returned false")
	}
	if !path(sampleFile, "writable") {
		t.Error("File is writable but returned false")
	}
	if !path(sampleFile, "existence") {
		t.Error("File exists but returned false")
	}
	if path(sampleFile, "executable") {
		t.Error("File is not executable but returned true")
	}
}

func TestRenderTemplateParsesSuccessfully(t *testing.T) {
	templateFile := "testResources/sampleLog4j.template"
	passed := renderTemplate(templateFile)
	assert.True(t, passed)
}
*/

func TestSplitToMapDefaults(t *testing.T) {
	defaultLoggers := "kafka=INFO,kafka.producer.async.DefaultEventHandler=DEBUG,state.change.logger=TRACE"
	loggers := "kafka.producer.async.DefaultEventHandler=ERROR,kafka.request.logger=WARN"
	result := splitToMapDefaults(",", defaultLoggers, loggers)
	assertEqual(result["kafka"], "INFO", t)
	assertEqual(result["kafka.producer.async.DefaultEventHandler"], "ERROR", t)
	assertEqual(result["kafka.request.logger"], "WARN", t)
	assertEqual(result["state.change.logger"], "TRACE", t)
}
