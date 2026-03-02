#!/bin/bash
# Commands to check pip versions in UBI8 container

echo "=== Check Python 3.14 pip (will fail - not available) ==="
yum list available python314-pip* 2>&1

echo -e "\n=== Check Python 3.12 pip (latest available) ==="
yum list available python3.12-pip* --showduplicates

echo -e "\n=== Check Python 3.11 pip ==="
yum list available python3.11-pip* --showduplicates

echo -e "\n=== Get exact version format for Python 3.12 pip ==="
yum list available python3.12-pip --showduplicates | grep python3.12-pip | head -1

echo -e "\n=== Alternative: Check all pip packages ==="
yum list available | grep -E "pip.*noarch" | grep -E "python3[0-9]|python3\.[0-9]"


