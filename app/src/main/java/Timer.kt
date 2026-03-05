import android.os.CountDownTimer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import java.util.Locale

class Timer : ViewModel() {
    var studyTime = 25
    var shortBreakTime = 5
    var longBreakTime = 10
    var longBreakInterval = 4

    var totalSessionCounter = 1
    var currentSessionCounter = 1

    enum class Progress { STUDY, SHORT_BREAK, LONG_BREAK }
    var currentProgress = mutableStateOf(Progress.STUDY)
    var timerStateText = mutableStateOf("START")
    var timerIsRunning = false
    var timerIsPaused = false

    var seconds = mapOf(
        Progress.STUDY to studyTime*60*1000L,
        Progress.SHORT_BREAK to shortBreakTime*60*1000L,
        Progress.LONG_BREAK to longBreakTime*60*1000L
    )

    var secondsLeft : Long = seconds[currentProgress.value]!!
    var secondsLeftText = mutableStateOf("25:00") // placeholder
    var timer : CountDownTimer? = null

    var showAlert = mutableStateOf(false)

    @Preview(showBackground=true)
    @Composable
    fun TimerSetup(modifier : Modifier = Modifier) {
        var _studyTime by remember { mutableStateOf(studyTime.toString()) }
        var _shortBreakTime by remember { mutableStateOf(shortBreakTime.toString()) }
        var _longBreakTime by remember { mutableStateOf(longBreakTime.toString()) }
        var _longBreakInterval by remember { mutableStateOf(longBreakInterval.toString()) }

        Column (modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(16.dp)) {
            Text(text = "TomaToro", modifier = Modifier.height(24.dp), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Time (minutes)", modifier = Modifier.systemBarsPadding())

            Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                // study
                OutlinedTextField(
                    value = _studyTime,
                    onValueChange = { newStudyTime -> _studyTime = newStudyTime
                        newStudyTime.toIntOrNull()?.let { studyTime = it }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Study", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.width(8.dp))

                // short break
                OutlinedTextField(
                    value = _shortBreakTime,
                    onValueChange = { newShortBreakTime -> _shortBreakTime = newShortBreakTime
                        newShortBreakTime.toIntOrNull()?.let { shortBreakTime = it }
                    },
                    modifier = Modifier.weight(1f),
                    label = {Text("Short Break", fontSize = 14.sp)},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.width(8.dp))

                // long break
                OutlinedTextField(
                    value = _longBreakTime,
                    onValueChange = {newLongBreakTime -> _longBreakTime = newLongBreakTime
                        newLongBreakTime.toIntOrNull()?.let { longBreakTime = it }
                    },
                    modifier = Modifier.weight(1f),
                    label = {Text("Long Break", fontSize = 14.sp)},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            Spacer(modifier = Modifier.height(2.dp))

            // long break interval
            OutlinedTextField(
                modifier = Modifier.width(175.dp),
                value = _longBreakInterval,
                onValueChange = { newLongBreakInterval -> _longBreakInterval = newLongBreakInterval
                    newLongBreakInterval.toIntOrNull()?.let { longBreakInterval = it }
                },
                label = {Text("Long Break Interval", fontSize = 14.sp)},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            Spacer(modifier = Modifier.height(24.dp))

            // timer
            Column (horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // session counter
                Text("Session #$totalSessionCounter", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                var currentProgressText = ""

                if (currentProgress.value == Progress.STUDY) {
                    currentProgressText = "STUDY"
                }
                else if (currentProgress.value == Progress.SHORT_BREAK) {
                    currentProgressText = "SHORT BREAK"
                }
                else {
                    currentProgressText = "LONG BREAK"
                }

                // current progress
                Text(currentProgressText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                // timer
                Text(secondsLeftText.value, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 64.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Row{
                    // change to stop if timer is running
                    Button(onClick = {
                        if (!timerIsRunning) {
                            timerStart()
                        }
                        else
                        {
                            timerStop()
                        }

                    }) {
                        Text(timerStateText.value)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { timerSkip() }) {
                        Text("SKIP")
                    }
                }
            }
        }

        var title = ""
        var message = ""

        if (currentProgress.value == Progress.STUDY) {
            title = "Break time over!"
            message = "Time to study!"
        }
        else {
            title = "Study time over!"
            message = "Time to take a break!"
        }

        if (showAlert.value) {
            AlertDialog(
                onDismissRequest = { showAlert.value = false },
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = { Button(onClick = { showAlert.value = false }) { Text("Okay") } }
            )
        }
    }

    fun timerStart () {
        timer?.cancel()

        timer = object : CountDownTimer(secondsLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsLeft = millisUntilFinished
                val minutes = millisUntilFinished/1000/60
                val seconds = millisUntilFinished/1000%60

                secondsLeftText.value = String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                // show alarm, set to next session
                showAlert.value = true
                nextSession()
            }
        }.start()

        timerStateText.value = "STOP"
        timerIsRunning = true
    }

    fun timerStop () {
        timer?.cancel()

        timerStateText.value = "START"
        timerIsRunning = false
    }

    fun timerSkip () {
        timer?.cancel()

        nextSession()

        timerStateText.value = "START"
        timerIsRunning = false
    }

    fun nextSession() {
        timerStop()

        if (currentProgress.value == Progress.STUDY) {
            if (currentSessionCounter % longBreakInterval == 0) {
                currentProgress.value = Progress.LONG_BREAK
            }
            else {
                currentProgress.value = Progress.SHORT_BREAK
            }
        }
        else {
            currentProgress.value = Progress.STUDY

            totalSessionCounter++
            currentSessionCounter++
        }

        if (currentSessionCounter >= longBreakInterval) {
            currentSessionCounter = 0
        }

        secondsLeft = when (currentProgress.value) {
            Progress.STUDY -> studyTime*60*1000L
            Progress.SHORT_BREAK -> shortBreakTime*60*1000L
            Progress.LONG_BREAK -> longBreakTime*60*1000L
        }

        val minutes = secondsLeft/1000/60
        val seconds = secondsLeft/1000%60

        secondsLeftText.value = String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds)
    }
}