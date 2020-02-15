import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class BeatBoxFinal {

    public static void main(String[] args) {
        new BeatBoxFinal().startUp("beishan");
    }

    JFrame frame;
    JPanel mainPanel;
    JList incomingList;
    JTextField userMessage;
    ArrayList<JCheckBox> checkBoxList;
    int nextNum;
    Vector<String> listVector = new Vector<String>();
    String userName;
    ObjectInputStream in;
    ObjectOutputStream out;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequence;
    Track track;

    String[] instrumentNames = {
            "Bass Drum","Closed Hi-Hat","Open Hi-Hat","Acoustic Snare"," Crash Cymbal","Hand Clap","High Tom","Hi Bongo",
            "Maracas","Whistle","Low Conga","Cowbell","Vibraslap","Low-mid Tom","High Agogo","Open Hi Conga"
    };

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public void startUp(String name){
        userName = name;
        try {
            Socket sock = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setUpMidi();
        buildGUI();
    }

    public void buildGUI(){
        frame = new JFrame("BeatBox");

        BorderLayout layout = new BorderLayout();
        JPanel backguound = new JPanel(layout);
        backguound.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkBoxList = new ArrayList<JCheckBox>();

        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        JButton sendIt = new JButton("SendIt");
        sendIt.addActionListener(new MySendListener());
        buttonBox.add(sendIt);


        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for(int i = 0; i < 16; i++) {
            nameBox.add(new JLabel(instrumentNames[i]));
        }

        backguound.add(BorderLayout.WEST, nameBox);
        backguound.add(BorderLayout.EAST, buttonBox);

        frame.getContentPane().add(backguound);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);

        backguound.add(BorderLayout.CENTER, mainPanel);

        for(int i = 0; i < 256; i++){
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100,100,300,300);
        frame.pack();
        frame.setVisible(true);
    }

    public void setUpMidi(){
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(20);
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart(){
        ArrayList<Integer> trackList = null;
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for(int i = 0; i < 16; i++){
            trackList = new ArrayList<Integer>();
            for(int j = 0; j < 16; j++){
                JCheckBox jc = (JCheckBox)checkBoxList.get(j + (i * 16));
                if(jc.isSelected()){
                    int key = instruments[i];
                    trackList.add(new Integer(key));
                } else{
                    trackList.add(null);
                }
            }
            makeTracks(trackList);
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void makeTracks(ArrayList list){
        Iterator it = list.iterator();
        for(int i = 0; i < 16; i++){
            Integer num = (Integer) it.next();
            if(num != null){
                int nukKey = num.intValue();
                track.add(makeEvent(144, 9, nukKey, 100, i));
                track.add(makeEvent(128, 9, nukKey, 100, i+1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick){
        MidiEvent event = null;
        ShortMessage a = new ShortMessage();
        try {
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        return event;
    }



    public class MyStartListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
        }
    }

    public class MySendListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            boolean[] checkboxState = new boolean[256];
            for(int i = 0; i < 256; i++){
                JCheckBox check = (JCheckBox) checkBoxList.get(i);
                if(check.isSelected()){
                    checkboxState[i] = true;
                }
            }
            String messageToSend = null;
            try {
                out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
                out.writeObject(checkboxState);
            } catch (IOException e) {
                System.out.println("Sorry dude, Could not send it to the server");
            }
        userMessage.setText("");
        }
    }

    public class MyListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent listSelectionEvent) {
            if(listSelectionEvent.getValueIsAdjusting()) {
                String selected = (String) incomingList.getSelectedValue();
                if(selected != null){
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    public class MyPlayMineListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if(mySequence != null){
                sequence = mySequence;
            }
        }
    }

    public class RemoteReader implements Runnable {

        boolean[] checkboxState = null;
        String nameToShow = null;
        Object obj = null;

        @Override
        public void run() {
           try {
               while((obj = in.readObject()) != null){
                   System.out.println("got an object from server");
                   System.out.println(obj.getClass());
                   String nameToShow = (String) obj;
                   checkboxState = (boolean[]) in.readObject();
                   otherSeqsMap.put(nameToShow, checkboxState);
                   listVector.add(nameToShow);
                   incomingList.setListData(listVector);
               }
           } catch (Exception ex) {
               ex.printStackTrace();
           }
        }
    }



    public void changeSequence(boolean[] checkboxState) {
        for(int i = 0; i < 256; i++){
            JCheckBox check = (JCheckBox) checkBoxList.get(i);
            if(checkboxState[i]){
                check.setSelected(true);
            } else{
                check.setSelected(false);
            }
        }
    }
}
