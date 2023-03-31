package com.example.charhoplayout;
import static com.example.charhoplayout.EarconManager.deleteChar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
//import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.bluetooth.BluetoothManager;
import com.tapwithus.sdk.bluetooth.TapBluetoothManager;
import com.tapwithus.sdk.mode.TapInputMode;
import com.tapwithus.sdk.mouse.MousePacket;
import com.tapwithus.sdk.tap.Tap;
import com.tapwithus.sdk.mode.RawSensorData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 1234;

    static final int MOUSE_SPEED = 5; // adjust as needed

    static final String OUTPUT_PREFIX = "key_input("; // the prefix of the output string for typing a letter
    /*
     * Static Variable Declaration
     * */
    public static TextToSpeech tts;         // Text To Speech Engine
    static boolean allowSearchScan=false;   // Scanning Characters in Edit Mode

    /*
     * Variable Declaration for Number Mode
     * */
    static boolean isNumberMode=false;
    static int numberModeToggle=0;


    /*
     * Variable Declaration for Special Character Mode
     * */
    static boolean isspecialCharMode=false;
    static int spModeToggle=0;

    static int toggle;

    /*
     * Variable Declaration for Auto-Suggestions Mode
     * */
    static boolean isAutoSuggestionMode=false;
    static int autoSuggestionModeToggle=0;
    ArrayList<String> SuggestionsResult = new ArrayList<String>();
    String str1 = "";
    String[] splited;

    /*
     * Buttons and TextView Declaration
     * */
    Button btnGetInfo,btnResetInfo;
    TextView tvInfo;

    /*
     * Count Total Taps Declaration
     * */
    CountTotalTaps countTotalTaps;

    Tap tap;

    HashMap<String, String> tapData;

    private boolean inScrollMode = false;
    private boolean inAlphabetMode = false;
    private char[] alphabet = " abcdefghijklmnopqrstuvwxyz ".toCharArray();
    int alphabetIndex = 0;

    private int alphabetCounter = 0;
    private boolean selectingAlphabet = false;
    private String selectedAlphabet = null;

    String glAlphabetString = "";
    String retAlphabetString = "";

    //private MouseManager mouseManager;
    //static boolean isMouseModeActive = false;
    static boolean isAlphabetModeActive = false;

    public void CustomMouseListener(Tap tap) {
        this.tap = tap;
        this.tapData = new HashMap<>();
        this.tapData.put("alphabet", "a"); // set the initial alphabet to "a"
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if the user has granted the BLUETOOTH_CONNECT permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // If the permission has not been granted, request it from the user
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
            }
        } else {
            // If the permission has already been granted, proceed with accessing the bonded devices
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        }

        BluetoothManager bluetoothManager = new BluetoothManager(getApplicationContext(), BluetoothAdapter.getDefaultAdapter()); // Initialise Bluetooth Manager
        TapBluetoothManager tapBluetoothManager = new TapBluetoothManager(bluetoothManager);


        TapSdk sdk = new TapSdk(tapBluetoothManager);     // Connect Bluetooth Manager with Tap Strap SDK
        TapSdkFactory.getDefault(getApplicationContext());  // Register TapSDK
        sdk.registerTapListener(mTap);


        tts = new TextToSpeech(this, MainActivity.this);  // Instantiate google Text-To-Speech Engine

        btnGetInfo = findViewById(R.id.btnGetInfo);
        btnResetInfo = findViewById(R.id.btnReset);
        tvInfo = findViewById(R.id.tvInfo);

        countTotalTaps = new CountTotalTaps(new HashMap<String, Integer>());

        btnGetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String res = countTotalTaps.displayTotalTapsCount();
                tvInfo.setText(res);
                tvInfo.setTextColor(Color.RED);
            }
        });

        btnResetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countTotalTaps = countTotalTaps.resetCounting();
                String res = countTotalTaps.displayTotalTapsCount();
                if (!retAlphabetString.isEmpty() || alphabetIndex == 0) {
                    retAlphabetString = "";
                    alphabetIndex = 0;
                }
                tvInfo.setText(res);
                tvInfo.setTextColor(Color.RED);
            }
        });
    }

    public TapListener mTap =new TapListener()
    {
        AlphabetMode alMode = new AlphabetMode(MainActivity.this);                 // Instantiate Alphabet Mode
        NumbersMode nmMode = new NumbersMode(MainActivity.this);                  // Instantiate Number Mode
        //NumbersMode nmMode = new NumbersMode(MainActivity.this);                  // Instantiate Number Mode
        SpecialCharactersMode specialCharMode = new SpecialCharactersMode(MainActivity.this); // Instantiate Special Char Mode
        TypedString tyString = new TypedString();                                       // Instantiate Typed String
        EditMode edMode = new EditMode(MainActivity.this);                       // Instantiate Edit Mode


        AutoSuggestionsMode autoSuggestionsMode = new AutoSuggestionsMode();           // Instantiate AutoSuggestion Mode

        //MouseListener mouseListener = new MouseListener();


        @Override
        public void onTapShiftSwitchReceived(String identifier, int shiftState) {
            // Implement your logic here
        }

        @Override
        public void onRawSensorInputReceived(String identifier, RawSensorData rawSensorData) {
            // Implement your logic here
        }

        @Override
        public void onTapChangedState(String identifier, int newState) {
            // Implement your logic here
        }


        @Override
        public void onBluetoothTurnedOn() {
        }

        @Override
        public void onBluetoothTurnedOff() {

        }

        @Override
        public void onTapStartConnecting(@NonNull String tapIdentifier) {

        }

        @Override
        public void onTapConnected(@NonNull String tapIdentifier) {
            tyString.typedStringInitialise();   // Initialise Typed String

            alMode.speakOut(tts,"Tap Strap connected to the phone. You can start keyflow");   // SpeakOut once tapStrap connected to phone

            //alMode.alModeInitialise();      // Initialise Alphabet Mode
            nmMode.nmModeInitialise();      // Initialise Number Mode
            specialCharMode.spModeInitialise(); // Initialise Special Char Mode

            EarconManager earconManager = new EarconManager();
            earconManager.setupEarcons(MainActivity.tts,getApplicationContext().getPackageName());

            CustomMouseListener(tap);

            if (!retAlphabetString.isEmpty() || alphabetIndex == 0) {
                retAlphabetString = "";
                glAlphabetString = "";
                alphabetIndex = 0;
                isAlphabetModeActive = false;
            }

        }

        @Override
        public void onTapDisconnected(@NonNull String tapIdentifier) {
            //alMode.speakOut(tts,"Tap strap not connected to the phone");       // SpeakOut once tapStrap unable to connect to phone
        }

        @Override
        public void onTapResumed(@NonNull String tapIdentifier) {

        }

        @Override
        public void onTapChanged(@NonNull String tapIdentifier) {

        }

        /*
        @Override
        public void onControllerModeStarted(@NonNull String tapIdentifier) {

        }

        @Override
        public void onTextModeStarted(@NonNull String tapIdentifier) {

        }*/

        @Override
        public void onTapInputReceived(@NonNull String tapIdentifier, int data, int repetetion) {
            EarconManager earconManager = new EarconManager();
            earconManager.setupEarcons(MainActivity.tts,getApplicationContext().getPackageName());

            Log.i("mktapinputreceived",tapIdentifier);
            Log.d("scroll false","data"+data);
            //inScrollMode = false;
            //Log.i("isAlphabetModeActive value ", ((int) isAlphabetModeActive));


            /*
             *  ###########Alphabet Mode Coding Starts Here################
             * */

            // alMode -> Forward
            if(!allowSearchScan & isAlphabetModeActive == false & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data==2)
            {
                tts.speak("Entered Alphabet Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                alMode.alModeInitialise();
                isAlphabetModeActive = true;

                // alMode.alModeForward(tts);

                //countTotalTaps.performCounting("alModeForward");
            }
            else if(isAlphabetModeActive == true & data == 2){
                selectingAlphabet = true;
                Log.d("Alphabet selected ","data"+data);

                retAlphabetString += glAlphabetString;

                System.out.println("glAlphabetString: " + glAlphabetString);
                System.out.println("retAlphabetString: " + retAlphabetString);

                if(glAlphabetString.equals(" ") ) {
                    tts.setPitch(1.5f);
                    tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                    Log.d("selection speak out space ", "alphabetIndex" + alphabetIndex);
                    tts.setPitch(1.0f);
                }
                else {
                    System.out.println("glAlphabetString is not space " + glAlphabetString);
                    tts.setPitch(1.5f);
                    tts.speak(glAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                    tts.setPitch(1.0f);
                }

                // need to store alphabet in a string array
            }
            // alMode -> Backward
            else if(!allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data == 4)
            {
                //alMode.alModeBackward(tts);

                //countTotalTaps.performCounting("alModeBackward");
            }
            // alMode -> Hopping
            else if(!allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data==6)
            {
                //alMode.alModeHopping(tts);

                //countTotalTaps.performCounting("alModeHopping");
            }
            // alMode -> Selection
            else if(!allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data==1)
            {
                //String results;
                //results = alMode.alModeSelect(tts,tyString.alreadyTyped/*,tyString.word*/);
                //tyString.alreadyTyped = results;

                //Log.d("TypedString",tyString.alreadyTyped);

                //countTotalTaps.performCounting("alModeSelection");
            }
            // alMode -> SpeakOut
            else if(!allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data == 15 || data == 14)
            {
                System.out.println("retAlphabetString: data is " + data);
                System.out.println("retAlphabetString: " + retAlphabetString);
                tts.setPitch(1.5f);
                tts.speak(retAlphabetString,TextToSpeech.QUEUE_FLUSH,null,null);
                tts.setPitch(1.0f);
                //alMode.alModeSpeakOut(tts,tyString.alreadyTyped);

                //countTotalTaps.performCounting("alModeSpeakOut");
            }
            // alMode ->Deletion
            else if(data==8)
            {
                retAlphabetString = retAlphabetString.substring(0, retAlphabetString.length() - 1);
                System.out.println(" Deletion retAlphabetString: " + retAlphabetString);

                /*
                String results;
                results = alMode.alModeDelete(tts,tyString.alreadyTyped);
                tyString.alreadyTyped = results;

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("alMOdeDeletion");
               */
            }
            // alMode -> Reset
            else if(!allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data == 8)
            {/*
                alMode.alModeReset(tts);

                countTotalTaps.performCounting("alModeReset");*/
            }

            /*
             *  ###########Number Mode Coding Starts Here####################
             * */

            // nmMode -> Enter
            /*
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==false & data==3)
            {
                tts.speak("Entered Number Mode",TextToSpeech.QUEUE_FLUSH,null,null);

                isNumberMode=true;
                numberModeToggle=1;
                nmMode.nmModeInitialise();

                countTotalTaps.performCounting("nmModeEnter");
            }
            //nmMode -> Exit
            else if(!allowSearchScan & isNumberMode==true & numberModeToggle==1 & isspecialCharMode==false & isAutoSuggestionMode==false & data==3) // nmMode -> Exit
            {
                tts.speak("Exit Number Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                isNumberMode=false;
                numberModeToggle=0;
                nmMode.numberHeadPoint = 0;

                countTotalTaps.performCounting("nmModeExit");
            }
            // nmMode -> Forward
            else if(!allowSearchScan & isNumberMode==true & numberModeToggle==1 & isspecialCharMode==false & isAutoSuggestionMode==false & data==2)
            {
                nmMode.nmModeForward(tts);

                countTotalTaps.performCounting("nmModeForward");
            }
            // nmMode -> Backward
            else if(!allowSearchScan & isNumberMode==true & numberModeToggle==1 & isspecialCharMode==false & isAutoSuggestionMode==false & data==4)
            {
                nmMode.nmModeBackward(tts);

                countTotalTaps.performCounting("nmModeBackward");
            }
            */
             /*
            // nmMode -> Selection
            else if(!allowSearchScan & isNumberMode==true & numberModeToggle==1 & isspecialCharMode==false & isAutoSuggestionMode==false & data==1)
            {
                String results;
                results = nmMode.nmModeSelection(tts,tyString.alreadyTyped/*,tyString.word);
                tyString.alreadyTyped = results;

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("nmModeSelection");
            }
            // nmMode -> Delete
            else if(!allowSearchScan & isNumberMode==true & numberModeToggle==1 & isspecialCharMode==false & isAutoSuggestionMode==false & data==16)
            {
                String results;
                results = nmMode.nmModeDeletion(tts,tyString.alreadyTyped/*,tyString.word);
                tyString.alreadyTyped = results;

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("nmModeDeletion");
            }
            //nmMode -> Speakout
            else if(!allowSearchScan & isNumberMode==true & numberModeToggle==1 & isspecialCharMode==false & isAutoSuggestionMode==false & data==30)
            {
                nmMode.nmModeSpeakOut(tts,tyString.alreadyTyped);

                countTotalTaps.performCounting("nmModeSpeakOut");
            }

            /*
             *  ###########Special Characters Mode Coding Starts Here###################
             * */

            // spMode -> Enter
            /*
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==false & data==5)
            {
                tts.speak("Entered Special Characters Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                isspecialCharMode=true;
                spModeToggle=1;
                specialCharMode.spModeInitialise();

                countTotalTaps.performCounting("specialCharModeEnter");
            }
            // spMode -> Exit
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==true & spModeToggle==1  & isAutoSuggestionMode==false & data==5)
            {
                tts.speak("Exit Special Characters Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                isspecialCharMode=false;
                spModeToggle=0;
                specialCharMode.spHeadPoint=0;

                countTotalTaps.performCounting("specialCharModeExit");
            }
            // spMode -> Forward
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==true & spModeToggle==1 & isAutoSuggestionMode==false & data==2)
            {
                specialCharMode.spModeForward(tts);

                countTotalTaps.performCounting("specialCharModeForward");
            }
            // spMode -> Backward
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==true & spModeToggle==1 & isAutoSuggestionMode==false & data==4)
            {
                specialCharMode.spModeBackward(tts);

                countTotalTaps.performCounting("specialCharModeBackward");
            }
            // spMode -> Selection
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==true & spModeToggle==1 & isAutoSuggestionMode==false & data==1)
            {
                String results;
                results = specialCharMode.spModeSelection(tts,tyString.alreadyTyped/*,tyString.word);
                tyString.alreadyTyped = results;

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("specialCharModeSelection");
            }
            // spMode -> Deletion
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==true & spModeToggle==1 & isAutoSuggestionMode==false & data==16)
            {
                String results;
                results = specialCharMode.spModeDeletion(tts,tyString.alreadyTyped/*,tyString.word);
                tyString.alreadyTyped = results;

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("specialCharModeDeletion");
            }
            //spMode -> Speakout
            else if (!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==true & spModeToggle==1 & isAutoSuggestionMode==false & data==30)
            {
                specialCharMode.spModeSpeakOut(tts,tyString.alreadyTyped);

                countTotalTaps.performCounting("specialCharModeSpeakOut");
            }

            /*
             *  ###########Edit Mode Coding Starts Here##########################
             * */

            /*
            //Index + Middle + Ring Finger ==> Enter In Edit Mode
            else if(!allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data== 14 & toggle==0)
            {
                //edMode.edModeInitialise(tts,tyString.alreadyTyped,getApplicationContext());

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("editModeEnter");
            }
            //Index + Middle + Ring ==> Exit Edit Mode
            else if(/*allowSearchScan==true &*/ /*isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data== 14 & toggle==1 & EditMode.editMode)
            {
                //tts.speak("Exit Edit Mode ",TextToSpeech.QUEUE_FLUSH,null,null);
                toggle=0;
                allowSearchScan=false;
                EditMode.editMode=false;

                countTotalTaps.performCounting("editModeExit");
            }
            //Index Finger to Navigate in Selected Word
            else if(allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false &  data == 2)
            {
                //edMode.edModeForwardNav(tts,tyString.alreadyTyped);

                countTotalTaps.performCounting("editModeForwardNav");
            }
            else if(allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false &  data == 4)
            {
                //edMode.edModeBackwardNav(tts,tyString.alreadyTyped);

                countTotalTaps.performCounting("editModeBackwardNav");
            }
            //RIng Finger for Decision Making
            else if(allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data==8)
            {
                //edMode.edModeDecisionNav(tts);

                countTotalTaps.performCounting("editModeDecisionNav");
            }
            // Decision Selection in Edit Mode
            else if(allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data ==1)
            {
                //edMode.edModeDecisionSelection(tts,tyString.alreadyTyped);

                countTotalTaps.performCounting("editModeDecisionSelection");
            }
            //Deletion in Edit Mode allowSearch Scan
            else if(allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data==16)
            {
                //tyString.alreadyTyped = edMode.edModeDeletion(tts,tyString.alreadyTyped);

                countTotalTaps.performCounting("editModeDeletion");
            }
            //SpeakOut in EditMode
            else if(allowSearchScan & isNumberMode==false & isspecialCharMode==false & isAutoSuggestionMode==false & data==30)
            {
                edMode.edModeSpeakOut(tts,tyString.alreadyTyped);

                countTotalTaps.performCounting("editModeSpeakOut");
            }
            /*
             *   #####Autosuggestions Mode
             * */
            /*
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==false & autoSuggestionModeToggle ==0 & data==9)
            {
                if (tyString.alreadyTyped.length() == 0)
                {
                    tts.speak("Nothing typed No Autosuggestions ",TextToSpeech.QUEUE_FLUSH,null,null);
                }
                else
                {
                    String str = tyString.alreadyTyped;
                    splited = str.split("\\s+");
                    str1 = splited[splited.length - 1];
                    SuggestionsResult = autoSuggestionsMode.fetchAutoSuggestions(getApplicationContext(),tts,str1);
                    if(SuggestionsResult != null)
                    {
                        isAutoSuggestionMode=true;
                    }
                }

                countTotalTaps.performCounting("autoSuggestionModeFetch");
            }
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==true & autoSuggestionModeToggle ==1 & data==9)
            {
                tts.speak("Exit AutoSuggestions Mode ",TextToSpeech.QUEUE_FLUSH,null,null);
                isAutoSuggestionMode=false;
                autoSuggestionModeToggle=0;

                countTotalTaps.performCounting("autoSuggestionModeExit");
            }
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==true & autoSuggestionModeToggle ==1 & data==2) // Forward Navigation in AutoSuggestion Mode
            {
                autoSuggestionsMode.forwardNavigateSuggestions(tts,SuggestionsResult);

                countTotalTaps.performCounting("autoSuggestionModeForwardNav");
            }
            else if(!allowSearchScan & isNumberMode==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==true & autoSuggestionModeToggle ==1 & data==1)//Selection in AutoSuggestion Mode
            {
                String word = autoSuggestionsMode.selectAutoSuggestion(tts);

                if(tyString.alreadyTyped.contains(" "))
                {
                    tyString.alreadyTyped = "";

                    String temp="";
                    for(int i=0; i < splited.length-1; i++)
                    {
                        if(temp.equals(""))
                        {
                            temp = temp + splited[i];
                            continue;
                        }
                        temp = temp +" "+splited[i];
                    }
                    tyString.alreadyTyped = temp+" "+word;
                }
                else
                {
                    tyString.alreadyTyped = word;
                }

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("autoSuggestionModeSelection");
            }*/
        }

        @Override
        public void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {

            Log.i("mkonMouseInputReceived",tapIdentifier);
            Log.d("mkMYINT","data"+data);
            //Log.d("mkmousedx","data"+data.dx.getInt());
            //Log.d("mkmousedy","data"+data.dy.getInt());
            //log(tapIdentifier+"mkmouseinputreceived"+data.dx.getInt()+","+data.dy.getInt());
            //log(tapIdentifier+"mkmouseinputreceived"+data.dx.getInt()+","+data.dy.getInt());

            if (data.dy.getInt() > 3 ){ //forward scroll
                inScrollMode = true;
                //tts.speak(" Alphabets scroll mode", TextToSpeech.QUEUE_FLUSH, null, null);

                if (isAlphabetModeActive == true) {
                    Log.d("indexfinger forward scroll - scroll through alphabets", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Falphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if(alphabetCounter > 19){
                        alphabetCounter = 0;
                        alphabetIndex++;
                        if (alphabetIndex < 0) {
                            alphabetIndex = 0;
                        }
                        else if (alphabetIndex >= alphabet.length) {
                            alphabetIndex = alphabet.length - 1;
                        }
                        if (alphabetIndex == 27){
                            Log.d("Fspeak out space ", "alphabetIndex" + alphabetIndex);
                            //Log.d("alphabet index value", "alphabetIndex" + alphabetIndex);
                            String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                            glAlphabetString = currentAlphabet;
                            System.out.println("FScrolling to: " + currentAlphabet);
                            tts.speak("space",TextToSpeech.QUEUE_FLUSH,null,null);
                            alphabetIndex = 0;//reset the position
                        }
                        else {
                            Log.d("Falphabet index value", "alphabetIndex" + alphabetIndex);
                            String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                            glAlphabetString = currentAlphabet;
                            System.out.println("FScrolling to: " + currentAlphabet);
                            System.out.println("FglAlphabetString: " + glAlphabetString);
                            tts.speak(currentAlphabet, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    else {
                        Log.i("Fmk do nothing",tapIdentifier);
                    }

                    /*
                    alphabetIndex++;
                    if (inScrollMode == true){
                        if (alphabetIndex < 0) {
                            alphabetIndex = 0;
                        }
                        else if (alphabetIndex >= alphabet.length) {
                            alphabetIndex = alphabet.length - 1;
                        }

                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        Log.d("alphabet index value", "alphabetIndex" + alphabetIndex);
                        String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                        System.out.println("Scrolling to: " + currentAlphabet);
                        tts.speak(currentAlphabet,TextToSpeech.QUEUE_FLUSH,null,null);
                    }
                     */
                    //tts.speak(" Alphabets scroll mode", TextToSpeech.QUEUE_FLUSH, null, null);
                    //Log.d("index finger scroll - scroll through alphabets", "data" + data.dy.getInt());
                    //String currentAlphabet = tapData.get("alphabet");
                    //int newAlphabetIndex = currentAlphabet.charAt(0) + data.dy.getInt() * MOUSE_SPEED;
                    //char newAlphabetChar = (char) newAlphabetIndex;

                    //String output = OUTPUT_PREFIX + newAlphabetChar + ")";
                    //tap.send(output);
                    //tapData.put("alphabet", Character.toString(newAlphabetChar));
                }
                else {
                    Log.i("FNot in  alphabet mode",tapIdentifier);
                }
            }
            else if (data.dy.getInt() < -3){ //backward scroll
                inScrollMode = true;
                if (isAlphabetModeActive == true) {
                    Log.d("indexfinger backward scroll - scroll through alphabets", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Balphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 19) {
                        alphabetCounter = 0;
                        alphabetIndex--;
                        if (alphabetIndex == 0) {
                            alphabetIndex = 27;
                        } else if (alphabetIndex >= alphabet.length) {
                            alphabetIndex = alphabet.length - 1;
                        }

                        if (alphabetIndex == 27) {
                            Log.d("Bspeak out space ", "alphabetIndex" + alphabetIndex);
                            //Log.d("alphabet index value", "alphabetIndex" + alphabetIndex);
                            String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                            glAlphabetString = currentAlphabet;
                            System.out.println("Scrolling to: " + currentAlphabet);
                            tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                            //alphabetIndex = 0;// no need reset the position
                        } else {
                            Log.d("Balphabet index value", "alphabetIndex" + alphabetIndex);
                            String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                            glAlphabetString = currentAlphabet;
                            System.out.println("BScrolling to: " + currentAlphabet);
                            System.out.println("BglAlphabetString: " + glAlphabetString);
                            tts.speak(currentAlphabet, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    } else {
                        Log.i("Bmk do nothing", tapIdentifier);
                    }
                }
            }
            else {
                inScrollMode = false;
                Log.d("no index finger scroll","data"+data.dy.getInt());
            }

        }

        @Override
        public void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data) {

        }

        @Override
        public void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {

        }
    };

    @Override
    public void onInit(int status) {
        if(status==TextToSpeech.SUCCESS)
        {
            int result= tts.setLanguage(Locale.US);
            tts.setSpeechRate(1.5f);
            tts.setPitch(1.0f);

            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Log.e("TTS","This Language is not Supported");
            }
        }
        else
        {
            Log.e("TTS","Initialization Failed! Activity");
        }
    }
}
