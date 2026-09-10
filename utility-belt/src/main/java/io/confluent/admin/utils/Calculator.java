/*
 * Copyright 2017 Confluent Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.admin.utils;

/**
 * Simple calculator utility for testing SonarQube coverage reporting.
 * This class demonstrates comprehensive test coverage for code quality analysis.
 */
public class Calculator {

  /**
   * Adds two integers.
   *
   * @param a first number
   * @param b second number
   * @return sum of a and b
   */
  public int add(int a, int b) {
    return a + b;
  }

  /**
   * Subtracts second integer from first.
   *
   * @param a first number
   * @param b second number
   * @return difference of a and b
   */
  public int subtract(int a, int b) {
    return a - b;
  }

  /**
   * Multiplies two integers.
   *
   * @param a first number
   * @param b second number
   * @return product of a and b
   */
  public int multiply(int a, int b) {
    return a * b;
  }

  /**
   * Divides first integer by second.
   *
   * @param a dividend
   * @param b divisor
   * @return quotient of a and b
   * @throws ArithmeticException if b is zero
   */
  public int divide(int a, int b) {
    if (b == 0) {
      throw new ArithmeticException("Division by zero is not allowed");
    }
    return a / b;
  }

  /**
   * Calculates factorial of a number.
   *
   * @param n non-negative integer
   * @return factorial of n
   * @throws IllegalArgumentException if n is negative
   */
  public long factorial(int n) {
    if (n < 0) {
      throw new IllegalArgumentException("Factorial is not defined for negative numbers");
    }
    if (n == 0 || n == 1) {
      return 1;
    }
    long result = 1;
    for (int i = 2; i <= n; i++) {
      result *= i;
    }
    return result;
  }

  /**
   * Checks if a number is even.
   *
   * @param n integer to check
   * @return true if n is even, false otherwise
   */
  public boolean isEven(int n) {
    return n % 2 == 0;
  }

  /**
   * Checks if a number is prime.
   *
   * @param n integer to check
   * @return true if n is prime, false otherwise
   */
  public boolean isPrime(int n) {
    if (n <= 1) {
      return false;
    }
    if (n == 2) {
      return true;
    }
    if (n % 2 == 0) {
      return false;
    }
    for (int i = 3; i * i <= n; i += 2) {
      if (n % i == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Finds the maximum of two integers.
   *
   * @param a first number
   * @param b second number
   * @return the larger of a and b
   */
  public int max(int a, int b) {
    return a > b ? a : b;
  }

  /**
   * Finds the minimum of two integers.
   *
   * @param a first number
   * @param b second number
   * @return the smaller of a and b
   */
  public int min(int a, int b) {
    return a < b ? a : b;
  }
}
