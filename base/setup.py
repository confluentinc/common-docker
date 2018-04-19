import setuptools


setuptools.setup(
    name='common-docker-tests',
    version='4.0.0',

    author="Confluent, Inc.",

    description='Docker image tests',

    url="https://github.com/confluentinc/common-docker",

    dependency_links=open('requirements.txt').read().split("\n"),

    packages=['test'],

    include_package_data=True,

    python_requires='>=2.7',
    setup_requires=['setuptools-git'],

)
