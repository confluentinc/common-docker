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

    def test_package_dedupe_exists(self):
        self.assertTrue(utils.path_exists_in_image(self.image, "/usr/bin/package_dedupe"))

    def test_package_dedupe_runnable(self):
        package_dedupe_cmd = "bash -c 'package_dedupe -h'"
        self.assertTrue(b"package_dedupe" in utils.run_docker_command(image=self.image, command=package_dedupe_cmd))

    def test_kafka_ready_jar(self):
        java_cmd = "bash -c 'java -cp \"/usr/share/java/cp-base-java-micro/*\" io.confluent.admin.utils.cli.KafkaReadyCommand -h'"
        self.assertTrue(b"Check if Kafka is ready" in utils.run_docker_command(image=self.image, command=java_cmd))

    def test_crypto_policy_config_is_fips(self):
        config_cmd = "bash -c 'cat /etc/crypto-policies/config'"
        self.assertEqual(b"FIPS\n", utils.run_docker_command(image=self.image, command=config_cmd))

    def test_crypto_policy_applied_is_fips(self):
        state_cmd = "bash -c 'cat /etc/crypto-policies/state/current'"
        self.assertEqual(b"FIPS\n", utils.run_docker_command(image=self.image, command=state_cmd))

    def test_crypto_policy_back_ends_link_to_fips(self):
        back_ends_cmd = "bash -c 'shopt -s nullglob; for f in /etc/crypto-policies/back-ends/*.config; do readlink -e \"$f\" || echo \"broken $f\"; done'"
        targets = utils.run_docker_command(image=self.image, command=back_ends_cmd).decode().splitlines()
        self.assertTrue(targets, "no back-end configs in /etc/crypto-policies/back-ends")
        for target in targets:
            self.assertTrue(target.startswith("/usr/share/crypto-policies/FIPS/"), target)

    def test_update_crypto_policies_not_installed(self):
        self.assertFalse(utils.path_exists_in_image(self.image, "/usr/bin/update-crypto-policies"))

    def test_python_not_installed(self):
        self.assertFalse(utils.path_exists_in_image(self.image, "/usr/bin/python3"))

    def test_libpython_not_installed(self):
        libpython_cmd = "bash -c 'compgen -G \"/usr/lib64/libpython*\" || echo none'"
        self.assertEqual(b"none\n", utils.run_docker_command(image=self.image, command=libpython_cmd))

    def test_python_directories_not_present(self):
        python_dir_cmd = "bash -c 'compgen -G \"/usr/lib*/python*\" || echo none'"
        self.assertEqual(b"none\n", utils.run_docker_command(image=self.image, command=python_dir_cmd))

if __name__ == '__main__':
    unittest.main()
