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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Calculator class.
 * This test aims for 100% code coverage to validate SonarQube reporting.
 */
@DisplayName("Calculator Test Suite")
public class CalculatorTest {

  private Calculator calculator;

  @BeforeEach
  void setUp() {
    calculator = new Calculator();
  }

  @Test
  @DisplayName("Addition: positive numbers")
  void testAddPositive() {
    assertEquals(5, calculator.add(2, 3));
    assertEquals(100, calculator.add(50, 50));
  }

  @Test
  @DisplayName("Addition: negative numbers")
  void testAddNegative() {
    assertEquals(-5, calculator.add(-2, -3));
    assertEquals(0, calculator.add(-5, 5));
  }

  @Test
  @DisplayName("Subtraction: basic cases")
  void testSubtract() {
    assertEquals(1, calculator.subtract(3, 2));
    assertEquals(-1, calculator.subtract(2, 3));
    assertEquals(0, calculator.subtract(5, 5));
  }

  @Test
  @DisplayName("Multiplication: basic cases")
  void testMultiply() {
    assertEquals(6, calculator.multiply(2, 3));
    assertEquals(0, calculator.multiply(5, 0));
    assertEquals(-10, calculator.multiply(5, -2));
  }

  @Test
  @DisplayName("Division: basic cases")
  void testDivide() {
    assertEquals(2, calculator.divide(6, 3));
    assertEquals(5, calculator.divide(25, 5));
    assertEquals(-2, calculator.divide(10, -5));
  }

  @Test
  @DisplayName("Division: throws exception on divide by zero")
  void testDivideByZero() {
    ArithmeticException exception = assertThrows(
        ArithmeticException.class,
        () -> calculator.divide(10, 0)
    );
    assertEquals("Division by zero is not allowed", exception.getMessage());
  }

  @Test
  @DisplayName("Factorial: base cases")
  void testFactorialBase() {
    assertEquals(1, calculator.factorial(0));
    assertEquals(1, calculator.factorial(1));
  }

  @Test
  @DisplayName("Factorial: positive numbers")
  void testFactorialPositive() {
    assertEquals(2, calculator.factorial(2));
    assertEquals(6, calculator.factorial(3));
    assertEquals(24, calculator.factorial(4));
    assertEquals(120, calculator.factorial(5));
  }

  @Test
  @DisplayName("Factorial: throws exception for negative numbers")
  void testFactorialNegative() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> calculator.factorial(-1)
    );
    assertEquals("Factorial is not defined for negative numbers", exception.getMessage());
  }

  @Test
  @DisplayName("IsEven: test even numbers")
  void testIsEven() {
    assertTrue(calculator.isEven(0));
    assertTrue(calculator.isEven(2));
    assertTrue(calculator.isEven(100));
    assertTrue(calculator.isEven(-4));
  }

  @Test
  @DisplayName("IsEven: test odd numbers")
  void testIsOdd() {
    assertFalse(calculator.isEven(1));
    assertFalse(calculator.isEven(3));
    assertFalse(calculator.isEven(99));
    assertFalse(calculator.isEven(-5));
  }

  @Test
  @DisplayName("IsPrime: test non-prime numbers")
  void testIsNotPrime() {
    assertFalse(calculator.isPrime(-1));
    assertFalse(calculator.isPrime(0));
    assertFalse(calculator.isPrime(1));
    assertFalse(calculator.isPrime(4));
    assertFalse(calculator.isPrime(6));
    assertFalse(calculator.isPrime(8));
    assertFalse(calculator.isPrime(9));
    assertFalse(calculator.isPrime(10));
  }

  @Test
  @DisplayName("IsPrime: test prime numbers")
  void testIsPrime() {
    assertTrue(calculator.isPrime(2));
    assertTrue(calculator.isPrime(3));
    assertTrue(calculator.isPrime(5));
    assertTrue(calculator.isPrime(7));
    assertTrue(calculator.isPrime(11));
    assertTrue(calculator.isPrime(13));
    assertTrue(calculator.isPrime(17));
    assertTrue(calculator.isPrime(19));
  }

  @Test
  @DisplayName("Max: various cases")
  void testMax() {
    assertEquals(5, calculator.max(3, 5));
    assertEquals(5, calculator.max(5, 3));
    assertEquals(7, calculator.max(7, 7));
    assertEquals(0, calculator.max(-5, 0));
  }

  @Test
  @DisplayName("Min: various cases")
  void testMin() {
    assertEquals(3, calculator.min(3, 5));
    assertEquals(3, calculator.min(5, 3));
    assertEquals(7, calculator.min(7, 7));
    assertEquals(-5, calculator.min(-5, 0));
  }
}
