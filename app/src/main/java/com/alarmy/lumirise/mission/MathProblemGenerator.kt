package com.alarmy.lumirise.mission

import kotlin.random.Random

/**
 * Generates math problems based on difficulty level.
 * - EASY: 1-digit addition/subtraction
 * - MEDIUM: multiplication/division
 * - HARD: mixed operations with parentheses
 */
class MathProblemGenerator(private val difficulty: MissionDifficulty) {

    private val usedProblems = mutableSetOf<String>()
    
    data class MathProblem(
        val question: String,
        val correctAnswer: Int,
        val choices: List<Int>
    )
    
    fun generateProblem(): MathProblem {
        val problem = when (difficulty) {
            MissionDifficulty.EASY -> generateEasyProblem()
            MissionDifficulty.MEDIUM -> generateMediumProblem()
            MissionDifficulty.HARD -> generateHardProblem()
        }
        
        // Store problem to avoid repetition
        usedProblems.add(problem.question)
        
        // Limit history size to prevent memory issues
        if (usedProblems.size > 100) {
            usedProblems.clear()
        }
        
        return problem
    }
    
    private fun generateEasyProblem(): MathProblem {
        val operations = listOf("+", "-")
        val operation = operations.random()
        
        var a: Int
        var b: Int
        
        when (operation) {
            "+" -> {
                a = Random.nextInt(1, 10)
                b = Random.nextInt(1, 10)
            }
            "-" -> {
                // Ensure positive result
                a = Random.nextInt(2, 10)
                b = Random.nextInt(1, a)
            }
            else -> {
                a = Random.nextInt(1, 10)
                b = Random.nextInt(1, 10)
            }
        }
        
        val question = "$a $operation $b"
        val answer = when (operation) {
            "+" -> a + b
            "-" -> a - b
            else -> a + b
        }
        
        return createProblemWithChoices(question, answer)
    }
    
    private fun generateMediumProblem(): MathProblem {
        val operations = listOf("*", "/")
        val operation = operations.random()
        
        var a: Int
        var b: Int
        
        when (operation) {
            "*" -> {
                a = Random.nextInt(2, 13)  // 2-12 multiplication table
                b = Random.nextInt(2, 13)
            }
            "/" -> {
                // Ensure clean division
                b = Random.nextInt(2, 10)
                val quotient = Random.nextInt(2, 13)
                a = b * quotient
            }
            else -> {
                a = Random.nextInt(2, 10)
                b = Random.nextInt(2, 10)
            }
        }
        
        val question = "$a $operation $b"
        val answer = when (operation) {
            "*" -> a * b
            "/" -> a / b
            else -> a + b
        }
        
        return createProblemWithChoices(question, answer)
    }
    
    private fun generateHardProblem(): MathProblem {
        val pattern = Random.nextInt(0, 3)
        
        return when (pattern) {
            0 -> generateTwoOperationProblem()
            1 -> generateParenthesesProblem()
            else -> generateMediumProblem()
        }
    }
    
    private fun generateTwoOperationProblem(): MathProblem {
        val ops = listOf("+", "-", "*")
        val op1 = ops.random()
        val op2 = ops.random()
        
        var a: Int
        var b: Int
        var c: Int
        
        // Simple two-operation: a op1 b op2 c
        a = Random.nextInt(2, 10)
        b = Random.nextInt(2, 10)
        c = Random.nextInt(2, 10)
        
        // Ensure multiplication doesn't overflow
        if (op1 == "*") {
            b = Random.nextInt(2, 8)
        }
        if (op2 == "*") {
            c = Random.nextInt(2, 8)
        }
        
        val question = "$a $op1 $b $op2 $c"
        val answer = when {
            op1 == "+" && op2 == "+" -> a + b + c
            op1 == "+" && op2 == "-" -> a + b - c
            op1 == "-" && op2 == "+" -> a - b + c
            op1 == "-" && op2 == "-" -> a - b - c
            op1 == "+" && op2 == "*" -> a + b * c
            op1 == "-" && op2 == "*" -> a - b * c
            op1 == "*" && op2 == "+" -> a * b + c
            op1 == "*" && op2 == "-" -> a * b - c
            op1 == "*" && op2 == "*" -> a * b * c
            else -> a + b + c
        }
        
        return createProblemWithChoices(question, answer)
    }
    
    private fun generateParenthesesProblem(): MathProblem {
        val types = listOf("(a+b)*c", "a*(b+c)", "(a+b)*(c+d)")
        val type = types.random()
        
        val question: String
        val answer: Int
        
        when (type) {
            "(a+b)*c" -> {
                val a = Random.nextInt(1, 6)
                val b = Random.nextInt(1, 6)
                val c = Random.nextInt(2, 6)
                question = "($a+$b)×$c"
                answer = (a + b) * c
            }
            "a*(b+c)" -> {
                val a = Random.nextInt(2, 6)
                val b = Random.nextInt(1, 6)
                val c = Random.nextInt(1, 6)
                question = "$a×($b+$c)"
                answer = a * (b + c)
            }
            "(a+b)*(c+d)" -> {
                val a = Random.nextInt(1, 5)
                val b = Random.nextInt(1, 5)
                val c = Random.nextInt(1, 5)
                val d = Random.nextInt(1, 5)
                question = "($a+$b)×($c+$d)"
                answer = (a + b) * (c + d)
            }
            else -> {
                val a = Random.nextInt(1, 10)
                val b = Random.nextInt(1, 10)
                question = "$a + $b"
                answer = a + b
            }
        }
        
        return createProblemWithChoices(question, answer)
    }
    
    private fun createProblemWithChoices(question: String, correctAnswer: Int): MathProblem {
        val choices = mutableListOf(correctAnswer)
        
        // Generate 3 wrong answers
        val variations = generateWrongAnswers(correctAnswer)
        choices.addAll(variations.take(3))
        
        // Shuffle choices
        val shuffledChoices = choices.shuffled()
        
        return MathProblem(
            question = question,
            correctAnswer = correctAnswer,
            choices = shuffledChoices
        )
    }
    
    private fun generateWrongAnswers(correctAnswer: Int): List<Int> {
        val wrongAnswers = mutableSetOf<Int>()
        
        while (wrongAnswers.size < 3) {
            val variation = when (Random.nextInt(0, 4)) {
                0 -> correctAnswer + Random.nextInt(1, 6)
                1 -> correctAnswer - Random.nextInt(1, 6)
                2 -> correctAnswer + Random.nextInt(-2, 3)
                else -> {
                    // Occasionally use related multiplication/division
                    if (correctAnswer > 1 && Random.nextBoolean()) {
                        correctAnswer * Random.nextInt(2, 4)
                    } else {
                        correctAnswer + Random.nextInt(-5, 6)
                    }
                }
            }
            
            // Ensure wrong answer is positive and different from correct
            if (variation > 0 && variation != correctAnswer) {
                wrongAnswers.add(variation)
            }
        }
        
        return wrongAnswers.toList()
    }
    
    fun reset() {
        usedProblems.clear()
    }
}
