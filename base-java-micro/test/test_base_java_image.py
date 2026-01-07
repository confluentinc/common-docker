import os
import unittest

import confluent.docker_utils as utils

class BaseJavaMicroImageTest(unittest.TestCase):

    def setUp(self):
        self.image = "{0}confluentinc/cp-base-java-micro:{1}".format(os.environ["DOCKER_REGISTRY"], os.environ["DOCKER_TAG"])

    def test_image_build(self):
        self.assertTrue(utils.image_exists(self.image))

    def test_ub_exists(self):
        self.assertTrue(utils.path_exists_in_image(self.image, "/usr/bin/ub"))

    def test_ub_runnable(self):
        ub_cmd = "bash -c 'ub -h'"
        self.assertTrue(b"utility commands" in utils.run_docker_command(image=self.image, command=ub_cmd))

    def test_package_dedupe_exits(self):
        self.assertTrue(utils.path_exists_in_image(self.image, "/usr/bin/package_dedupe"))

    def test_package_dedupe_runnable(self):
        package_dedupe_cmd = "bash -c 'package_dedupe -h'"
        self.assertTrue(b"package_dedupe" in utils.run_docker_command(image=self.image, command=package_dedupe_cmd))

    def test_kafka_ready_jar(self):
        java_cmd = "bash -c 'java -cp \"/usr/share/java/cp-base-java-micro/*\" io.confluent.admin.utils.cli.KafkaReadyCommand -h'"
        self.assertTrue(b"Check if Kafka is ready" in utils.run_docker_command(image=self.image, command=java_cmd))

if __name__ == '__main__':
    unittest.main()
