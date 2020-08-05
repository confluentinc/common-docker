import os
import unittest

import confluent.docker_utils as utils


class BaseImageTest(unittest.TestCase):

    def setUp(self):
        self.image = "{0}confluentinc/cp-base-new:{1}".format(os.environ["DOCKER_REGISTRY"], os.environ["DOCKER_TAG"])

    def test_image_build(self):
        self.assertTrue(utils.image_exists(self.image))

    def test_dub_exists(self):
        self.assertTrue(utils.path_exists_in_image(self.image, "/usr/local/bin/dub"))
        self.assertTrue(utils.path_exists_in_image(self.image, "/usr/local/bin/cub"))

    def test_cub_dub_runable(self):
        dub_cmd = "bash -c '/usr/local/bin/dub --help'"
        cub_cmd = "bash -c '/usr/local/bin/cub --help'"
        self.assertTrue(b"Docker Utility Belt" in utils.run_docker_command(image=self.image, command=dub_cmd))
        self.assertTrue(b"Confluent Platform Utility Belt." in utils.run_docker_command(image=self.image, command=cub_cmd))


if __name__ == '__main__':
    unittest.main()
