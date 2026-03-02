#!/bin/bash
# Script to check Python versions available in UBI8 container
# Usage: Run this inside a UBI8 container or use: docker run -it registry.access.redhat.com/ubi8/ubi-minimal:latest bash

echo "=== Checking UBI8 Base Image ==="
cat /etc/os-release

echo -e "\n=== Checking available Python packages in repositories ==="
echo "Searching for python3* packages:"
dnf search python3 2>/dev/null | grep -E "^python3[0-9]|^python3[0-9]-" || yum search python3 2>/dev/null | grep -E "^python3[0-9]|^python3[0-9]-" || echo "Note: dnf/yum may not be available in minimal image"

echo -e "\n=== Checking specific Python 3.14 packages ==="
dnf list available python314* 2>/dev/null || yum list available python314* 2>/dev/null || echo "python314 packages not found"

echo -e "\n=== Checking all Python-related packages ==="
dnf list available | grep -i python 2>/dev/null || yum list available | grep -i python 2>/dev/null || echo "No Python packages found"

echo -e "\n=== Checking pip packages ==="
dnf list available | grep -i "pip" 2>/dev/null || yum list available | grep -i "pip" 2>/dev/null || echo "No pip packages found"

echo -e "\n=== Checking enabled repositories ==="
dnf repolist 2>/dev/null || yum repolist 2>/dev/null || echo "Cannot list repositories"

echo -e "\n=== If dnf/yum not available, try with microdnf ==="
microdnf search python3 2>/dev/null | head -20 || echo "microdnf search not available"


