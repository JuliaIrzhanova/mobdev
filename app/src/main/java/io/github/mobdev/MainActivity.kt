package io.github.mobdev

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView

    private var currentInput = ""
    private var firstValue: Double? = null
    private var pendingOperation: String? = null
    private var resetInputOnNextDigit = false
    private var errorShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        if (savedInstanceState != null) {
            currentInput = savedInstanceState.getString(KEY_CURRENT_INPUT, "")
            if (savedInstanceState.containsKey(KEY_FIRST_VALUE)) {
                firstValue = savedInstanceState.getDouble(KEY_FIRST_VALUE)
            }
            pendingOperation = savedInstanceState.getString(KEY_PENDING_OPERATION)
            resetInputOnNextDigit = savedInstanceState.getBoolean(KEY_RESET_INPUT, false)
            errorShown = savedInstanceState.getBoolean(KEY_ERROR_SHOWN, false)
        }

        setInputButton(R.id.btn0, "0")
        setInputButton(R.id.btn1, "1")
        setInputButton(R.id.btn2, "2")
        setInputButton(R.id.btn3, "3")
        setInputButton(R.id.btn4, "4")
        setInputButton(R.id.btn5, "5")
        setInputButton(R.id.btn6, "6")
        setInputButton(R.id.btn7, "7")
        setInputButton(R.id.btn8, "8")
        setInputButton(R.id.btn9, "9")
        setInputButton(R.id.btnDot, ".")

        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperationClick("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperationClick("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperationClick("*") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperationClick("/") }

        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEqualsClick() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }

        updateDisplay()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_INPUT, currentInput)
        if (firstValue != null) {
            outState.putDouble(KEY_FIRST_VALUE, firstValue!!)
        }
        outState.putString(KEY_PENDING_OPERATION, pendingOperation)
        outState.putBoolean(KEY_RESET_INPUT, resetInputOnNextDigit)
        outState.putBoolean(KEY_ERROR_SHOWN, errorShown)
    }

    private fun setInputButton(buttonId: Int, value: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            appendToInput(value)
        }
    }

    private fun appendToInput(value: String) {
        if (errorShown) {
            clearAll()
        }

        if (resetInputOnNextDigit) {
            currentInput = ""
            resetInputOnNextDigit = false
        }

        if (value == ".") {
            if (currentInput.isEmpty()) {
                currentInput = "0."
            } else if (!currentInput.contains(".")) {
                currentInput += "."
            }
        } else {
            if (currentInput == "0") {
                currentInput = value
            } else {
                currentInput += value
            }
        }

        updateDisplay()
    }

    private fun onOperationClick(operation: String) {
        if (errorShown) {
            return
        }

        if (currentInput.isEmpty()) {
            if (firstValue != null) {
                pendingOperation = operation
                updateDisplay()
            }
            return
        }

        if (firstValue == null) {
            firstValue = currentInput.toDouble()
        } else if (pendingOperation != null && !resetInputOnNextDigit) {
            val secondValue = currentInput.toDouble()
            val result = calculate(firstValue!!, secondValue, pendingOperation!!)
            if (result == null) {
                showError()
                return
            }
            firstValue = result
            currentInput = formatNumber(result)
        }

        pendingOperation = operation
        resetInputOnNextDigit = true
        updateDisplay()
    }

    private fun onEqualsClick() {
        if (errorShown) {
            return
        }

        if (firstValue == null || pendingOperation == null || currentInput.isEmpty()) {
            return
        }

        val secondValue = currentInput.toDouble()
        val result = calculate(firstValue!!, secondValue, pendingOperation!!)

        if (result == null) {
            showError()
            return
        }

        currentInput = formatNumber(result)
        firstValue = null
        pendingOperation = null
        resetInputOnNextDigit = true
        updateDisplay()
    }

    private fun calculate(a: Double, b: Double, operation: String): Double? {
        return when (operation) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) null else a / b
            else -> null
        }
    }

    private fun backspace() {
        if (errorShown) {
            clearAll()
            return
        }

        if (resetInputOnNextDigit) {
            return
        }

        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun clearAll() {
        currentInput = ""
        firstValue = null
        pendingOperation = null
        resetInputOnNextDigit = false
        errorShown = false
        updateDisplay()
    }

    private fun updateDisplay() {
        if (errorShown) {
            tvExpression.text = ""
            tvResult.text = "Error"
            return
        }

        tvExpression.text = buildExpressionText()
        tvResult.text = if (currentInput.isEmpty()) "0" else currentInput
    }

    private fun buildExpressionText(): String {
        val firstText = if (firstValue != null) formatNumber(firstValue!!) else ""
        val opText = pendingOperation ?: ""
        return listOf(firstText, opText).filter { it.isNotEmpty() }.joinToString(" ")
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun showError() {
        currentInput = ""
        firstValue = null
        pendingOperation = null
        resetInputOnNextDigit = false
        errorShown = true
        updateDisplay()
    }

    companion object {
        private const val KEY_CURRENT_INPUT = "currentInput"
        private const val KEY_FIRST_VALUE = "firstValue"
        private const val KEY_PENDING_OPERATION = "pendingOperation"
        private const val KEY_RESET_INPUT = "resetInputOnNextDigit"
        private const val KEY_ERROR_SHOWN = "errorShown"
    }
}