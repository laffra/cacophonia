package cacophonia.ui;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

/**
 * Creates sounds for plugins.
 */
class Orchestra {
	MidiChannel midiChannel;
	int volume;
	Synthesizer synthesizer;
	    
	public Orchestra() {
		try {
			synthesizer = MidiSystem.getSynthesizer();
			synthesizer.open();
			MidiChannel[] allChannels = synthesizer.getChannels();
			midiChannel = allChannels[0];
		} catch (MidiUnavailableException e) {
			throw new IllegalStateException("Midi support is not available!");
		}
		volume = 80;
	}
	
	public void setVolume(int volumeLevel) {
		volume = volumeLevel;
	}
	
	public void noteOn(int noteNumber) {
		if (!UI.muted) {
			midiChannel.noteOn( noteNumber, volume);
		}
	}
	
	public void noteOff(int noteNumber) {
		midiChannel.noteOff( noteNumber );
	}
	
	public void allNotesOff() {
		midiChannel.allNotesOff();
	}
	
	public void setInstrument(int instrumentNumber) {
		midiChannel.programChange(instrumentNumber);
	}	
}