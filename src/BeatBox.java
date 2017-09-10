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


/**
 * Created by adavi on 07.09.2017.
 */
public class BeatBox {

    JFrame theFrame;
    JPanel mainPanel;
    JList incomingList;
    JTextField userMessage;
    ArrayList<JCheckBox> checkBoxList;
    int nextNum;
    Vector<String> listVector = new Vector<>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();

    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequence = null;
    Track track;


    String[] instrumentNames = {"Bass Drum", "Closed HI-Hat", "Open Hi-Hat",
            "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo",
            "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
            "High Agogo", "Open Hi Conga"};

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

        public static void main(String[] args) {

            new BeatBox().startUp(args[0]);


        }

    public void startUp(String name) {
        userName = name;
        System.out.println("yfvhfvuyfy");
        try {
            Socket sock = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());


            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (Exception e) {
            System.out.println("couldn't connect -- you will have to play alone");
        }
        setUpMidi();
        buildGUI();
    }


    private void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkBoxList = new ArrayList<JCheckBox>();

        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);


        JButton stop = new JButton("Stop");
        start.addActionListener(new MyStopListener());
        buttonBox.add(stop);


        JButton upTempo = new JButton("Tempo Up");
        start.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        start.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton sendId = new JButton("Send id");
        sendId.addActionListener(new MySendIdListener());
        buttonBox.add(sendId);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);
        GridLayout grit = new GridLayout(16, 16);
        grit.setVgap(1);
        grit.setVgap(2);
        mainPanel = new JPanel(grit);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }
        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);

    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart() {
        ArrayList<Integer> trackList = null;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<Integer>();

            for (int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkBoxList.get(j + (16 * i));

                if (jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(new Integer(key));

                } else {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

    } public class MyStartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            buildTrackAndStart();
        }
    }
    public class MyStopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            sequencer.stop();

        }
    }
    public class MyUpTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            float tempoFactor = sequencer.getTempoFactor();

            sequencer.setTempoFactor((float) (tempoFactor * 1.03));

        }
    }
    public class MyDownTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * .97));
        }
    }
    public class MySendIdListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean [] checkBoxState = new boolean[256];

            for (int i = 0; i < 256; i++){
                JCheckBox check =  (JCheckBox)checkBoxList.get(i);

                if (check.isSelected()){
                    checkBoxState[i] = true;
                }
            }
            String messageToSend = null;
            try {
                out.writeObject(userName + nextNum++ +": "+ userMessage.getText());
                out.writeObject(checkBoxState);
            } catch (IOException e1) {
                System.out.println("Sorry dude. Could not send it to the server.");
            }
            userMessage.setText("");

        }
    }
    public class MyListSelectionListener implements ListSelectionListener {


        @Override
        public void valueChanged(ListSelectionEvent le) {

            if (!le.getValueIsAdjusting()){
                String selected = (String)incomingList.getSelectedValue();
                if(selected != null){
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }



    public class RemoteReader implements Runnable {

        boolean[] checkBoxState = null;
        String nameToShow = null;
        Object object = null;
        @Override
        public void run() {
            try {
                while((object = in.readObject()) != null){
                    System.out.println("got an object from server");
                    System.out.println(object.getClass());
                    nameToShow = (String) object;
                    checkBoxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow,checkBoxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    public class MyPlayMineListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (mySequence != null){
                sequence = mySequence;
            }
        }


    }

    public void changeSequence(boolean[] checkBoxState){
        for (int i = 0; i < 256; i++){
            JCheckBox check = (JCheckBox) checkBoxList.get(i);
            if (checkBoxState[i]){
                check.setSelected(true);
            }else{
                check.setSelected(false);
            }
        }
    }

    public void makeTracks(ArrayList list){
        Iterator iterator = list.iterator();
        for (int i = 0; i < 16; i++){
            Integer num = (Integer) iterator.next();
            if (num != null){
                int numKey = num.intValue();
                track.add(makeEvent(144,9,numKey,100,i));
                track.add(makeEvent(128,9,numKey,100,i +1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;

        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd,chan,one,two);
            event = new MidiEvent(a,tick);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        return event;
    }



}

