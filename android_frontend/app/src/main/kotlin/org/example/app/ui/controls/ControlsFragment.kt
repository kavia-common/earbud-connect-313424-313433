package org.example.app.ui.controls

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.example.app.R

class ControlsFragment : Fragment() {

    private var audioManager: AudioManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_controls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val info = view.findViewById<TextView>(R.id.tvControlsInfo)
        val seek = view.findViewById<SeekBar>(R.id.seekVolume)
        val btn = view.findViewById<Button>(R.id.btnPlayPause)

        val am = audioManager
        if (am == null) {
            info.visibility = View.VISIBLE
            seek.isEnabled = false
            btn.isEnabled = false
            return
        }

        // Volume control (system music stream).
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        seek.max = max
        seek.progress = current

        info.text = "Volume uses system music stream. Play/Pause is best-effort and may not control earbuds directly."

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Play/pause: send media key event (works if a media session is active).
        btn.setOnClickListener {
            try {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                @Suppress("DEPRECATION")
                am.dispatchMediaKeyEvent(down)
                @Suppress("DEPRECATION")
                am.dispatchMediaKeyEvent(up)
            } catch (_: Throwable) {
                // If dispatch fails, keep UI responsive but no action.
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioManager = null
    }

    companion object {
        fun newInstance(): ControlsFragment = ControlsFragment()
    }
}
