import os
import unittest

import confluent.docker_utils as utils


class BaseRefreshImageTest(unittest.TestCase):

    def setUp(self):
        self.image = "{0}confluentinc/cp-base-refresh:{1}".format(os.environ["DOCKER_REGISTRY"], os.environ["DOCKER_TAG"])

    def test_image_build(self):
        self.assertTrue(utils.image_exists(self.image))

    def test_jre_17_installed(self):
        jre_cmd = "java --version"
        result = utils.run_docker_command(image=self.image, command=jre_cmd)
        self.assertTrue(b'17' in result)

    def test_ub_exists(self):
        self.assertTrue(utils.path_exists_in_image(self.image, "/usr/bin/ub"))

    def test_ub_runnable(self):
        ub_cmd = "bash -c '/usr/bin/ub -h'"
        self.assertTrue(b"utility commands" in utils.run_docker_command(image=self.image, command=ub_cmd))    

if __name__ == '__main__':
    unittest.main()