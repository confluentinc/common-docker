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

if __name__ == '__main__':
    unittest.main()