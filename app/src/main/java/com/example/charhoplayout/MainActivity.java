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

import com.tapwithus.sdk.BuildConfig;
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
    private boolean inNumberMode = false;
    private char[] alphabet = " abcdefghijklmnopqrstuvwxyz ".toCharArray();
    private char[] number = " 0123456789".toCharArray();

    private char[] specialCharacters = "&.@!*#$%?".toCharArray();
    int alphabetIndex = 0;
    int numberIndex = 0;

    private int alphabetCounter = 0;
    private int numberCounter = 0;
    //Auto Suggestion counter
    public int autoSuggestionCounter = 0;

    private int specialCharacterCounter = 0;
    private boolean selectingAlphabet = false;
    private String selectedAlphabet = null;

    String glAlphabetString = "";
    String retAlphabetString = "";

    //private MouseManager mouseManager;
    //static boolean isMouseModeActive = false;
    static boolean isAlphabetModeActive = false;
    static boolean isNumberModeActive = false;

    //Variables for edit Mode
    static boolean isEditModeReplaceActive = false;
    static boolean isEditModeInsertActive = false;
    private int editModeIndex = 0;
    private int editModeIndexBuffer = 0;
    private int editStringLength = 0;

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
            //nmMode.nmModeInitialise();      // Initialise Number Mode
            //specialCharMode.spModeInitialise(); // Initialise Special Char Mode

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
        public synchronized void onTapInputReceived(@NonNull String tapIdentifier, int data, int repetetion) {
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
            if(!allowSearchScan & isAlphabetModeActive == false  & data==2)
            {
                tts.speak("Entered Alphabet Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                //alMode.alModeInitialise();
                isAlphabetModeActive = true;
                isNumberModeActive = false;
                isAutoSuggestionMode = false;
                isspecialCharMode = false;
                //isEditModeReplaceActive=false;
                //isEditModeInsertActive=false;

                // alMode.alModeForward(tts);

                //countTotalTaps.performCounting("alModeForward");
            }
            else if(isAlphabetModeActive == true & data == 2){
                //selectingAlphabet = true;
                Log.d("Alphabet selected ","data"+data);

                /* retAlphabetString += glAlphabetString;*/


                if(isEditModeReplaceActive == false & isEditModeInsertActive == false){
                    retAlphabetString += glAlphabetString;
                }
                else if(isEditModeReplaceActive==true & isEditModeInsertActive == false){
                    char[] charArray  = retAlphabetString.toCharArray();
                    System.out.println("isEditModeReplaceActive true - charArray: " + Arrays.toString(charArray));
                    System.out.println("isEditModeReplaceActive true - glAlphabetString 0: " + glAlphabetString.charAt(0));
                    System.out.println("isEditModeReplaceActive true - charArray editModeIndexBuffer: " + charArray[editModeIndexBuffer]);
                    charArray[editModeIndexBuffer] = glAlphabetString.charAt(0);
                    retAlphabetString = new String(charArray);
                }

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
            // alMode -> SpeakOut
            else if(data ==30)
            {
                System.out.println("retAlphabetString: data is " + data);
                System.out.println("retAlphabetString: " + retAlphabetString);
                tts.setPitch(1.5f);
                //tts.speak(retAlphabetString,TextToSpeech.QUEUE_FLUSH,null,null);
                if (retAlphabetString.length()==0) {
                    tts.speak("No message typed", TextToSpeech.QUEUE_FLUSH, null, null);
                }
                else
                {
                    tts.speak(retAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                }
                tts.setPitch(1.0f);
                //alMode.alModeSpeakOut(tts,tyString.alreadyTyped);

                //countTotalTaps.performCounting("alModeSpeakOut");
            }
            // alMode ->Deletion
            else if(data==24)
            {
                String deleted_alphabet = String.valueOf(retAlphabetString.charAt(retAlphabetString.length() - 1));
                System.out.println(" Deletion retAlphabetString: " + retAlphabetString);
                //tts.speak(deleted_alphabet,TextToSpeech.QUEUE_ADD,null,null);
                tts.speak(deleted_alphabet+"", TextToSpeech.QUEUE_ADD, null, null);
                retAlphabetString = retAlphabetString.substring(0, retAlphabetString.length() - 1);
                tts.playEarcon(deleteChar,TextToSpeech.QUEUE_FLUSH,null,null);

                //retAlphabetString = retAlphabetString.substring(0, retAlphabetString.length() - 1);
                //System.out.println(" Deletion retAlphabetString: " + retAlphabetString);

                /*
                String results;
                results = alMode.alModeDelete(tts,tyString.alreadyTyped);
                tyString.alreadyTyped = results;

                Log.d("TypedString",tyString.alreadyTyped);

                countTotalTaps.performCounting("alMOdeDeletion");
               */
            }
            /*
             *  ###########Number Mode Coding Starts Here####################
             * */

            // nmMode -> Enter

            else if(isNumberModeActive == false & data==4)
            {
                tts.speak("Entered Number Mode",TextToSpeech.QUEUE_FLUSH,null,null);

                Log.d("Number mode entered","data"+data);

                isNumberModeActive = true;

                isAlphabetModeActive = false;

                isAutoSuggestionMode=false;

                isspecialCharMode = false;

                //isEditModeReplaceActive=false;
                //isEditModeInsertActive=false;


                //numberModeToggle=1;
                //nmMode.nmModeInitialise();

                //countTotalTaps.performCounting("nmModeEnter");
            }
            else if(isNumberModeActive == true & data == 4){
                selectingAlphabet = true;
                Log.d("Alphabet selected ","data"+data);
                /*retAlphabetString += glAlphabetString;*/

                if(isEditModeReplaceActive == false & isEditModeInsertActive == false){
                    retAlphabetString += glAlphabetString;
                }
                else if(isEditModeReplaceActive==true & isEditModeInsertActive == false){
                    char[] charArray = retAlphabetString.toCharArray();
                    charArray[editModeIndexBuffer] = glAlphabetString.charAt(0);
                    retAlphabetString = new String(charArray);
                }

                System.out.println("glAlphabetString: " + glAlphabetString);
                System.out.println("retAlphabetString: " + retAlphabetString);

                tts.setPitch(1.5f);
                tts.speak(glAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                tts.setPitch(1.0f);

                // need to store alphabet in a string array
            }
            /*
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

            else if(isspecialCharMode==false & data==8)
            {
                tts.speak("Entered Special Characters Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Entered Special Characters Mode","data"+data);

                isspecialCharMode=true;
                isAlphabetModeActive = false;
                isNumberMode = false;
                isAutoSuggestionMode = false;
                //isEditModeReplaceActive=false;
                //isEditModeInsertActive=false;

                //spModeToggle=1;
                //specialCharMode.spModeInitialise();

                //countTotalTaps.performCounting("specialCharModeEnter");
            }
            else if(isspecialCharMode == true & data == 8){
                //selectingAlphabet = true;
                Log.d("Special Character selected ","data"+data);
                /*retAlphabetString += glAlphabetString;*/

                if(isEditModeReplaceActive == false & isEditModeInsertActive == false){
                    retAlphabetString += glAlphabetString;
                }
                else if(isEditModeReplaceActive==true & isEditModeInsertActive == false){
                    char[] charArray = retAlphabetString.toCharArray();
                    charArray[editModeIndexBuffer] = glAlphabetString.charAt(0);
                    retAlphabetString = new String(charArray);
                }

                System.out.println("glAlphabetString: " + glAlphabetString);
                System.out.println("retAlphabetString: " + retAlphabetString);

                if(glAlphabetString.equals("#"))
                {
                    tts.setPitch(1.5f);
                    tts.speak("Hash",TextToSpeech.QUEUE_FLUSH,null,null);
                    tts.setPitch(1.0f);
                }
                else if(glAlphabetString.equals("$"))
                {
                    tts.setPitch(1.5f);
                    tts.speak("Dollar",TextToSpeech.QUEUE_FLUSH,null,null);
                    tts.setPitch(1.0f);
                }
                else
                {
                    tts.setPitch(1.5f);
                    tts.speak(glAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                    tts.setPitch(1.0f);
                }
            }
            else if(isEditModeReplaceActive == false & data == 6){
                tts.speak("Entered Edit Replace Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Entered Edit Replace Mode","data"+data);

                isEditModeReplaceActive=true;
                isEditModeInsertActive=false;
                isspecialCharMode=false;
                isAlphabetModeActive = false;
                isNumberMode = false;
                isAutoSuggestionMode = false;
                editStringLength = retAlphabetString.length();
            }
            else if(isEditModeReplaceActive == true & data == 6){
                isEditModeReplaceActive=false;/*TODO: Exit mode should automatically happen when user sets to a different mode. */
                tts.speak("Exit Edit Replace Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Exit Edit Replace Mode","data"+data);
            }
            else if(isEditModeInsertActive == false & data == 12){
                tts.speak("Entered Edit Insert Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Entered Edit Insert Mode","data"+data);

                isEditModeReplaceActive=false;
                isEditModeInsertActive=true;
                isspecialCharMode=false;
                isAlphabetModeActive = false;
                isNumberMode = false;
                isAutoSuggestionMode = false;
                editStringLength = retAlphabetString.length();
            }

            /*
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

            else if(isAutoSuggestionMode==false & data==14)
            {
                isspecialCharMode=false;
                isAlphabetModeActive = false;
                isNumberMode = false;
                isAutoSuggestionMode = true;

                if (retAlphabetString.length() == 0)
                {
                    tts.speak("Nothing typed No Autosuggestions ",TextToSpeech.QUEUE_FLUSH,null,null);
                }
                else
                {
                    String str = retAlphabetString;
                    splited = str.split("\\s+");
                    str1 = splited[splited.length - 1];
                    SuggestionsResult = autoSuggestionsMode.fetchAutoSuggestions(getApplicationContext(),tts,str1);
                    if(SuggestionsResult != null)
                    {
                        System.out.println("isAutoSuggestionMode is true");
                        isAutoSuggestionMode=true;
                        isAlphabetModeActive = false;
                    }
                    else {
                        System.out.println("isAutoSuggestionMode SuggestionsResult is null");
                    }
                }

                //countTotalTaps.performCounting("autoSuggestionModeFetch");
            }/*
            else if(!allowSearchScan & isNumberModeActive==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==true & autoSuggestionModeToggle ==1 & data==14)
            {
                tts.speak("Exit AutoSuggestions Mode ",TextToSpeech.QUEUE_FLUSH,null,null);
                isAutoSuggestionMode=false;
                autoSuggestionModeToggle=0;

                countTotalTaps.performCounting("autoSuggestionModeExit");
            }
            else if(!allowSearchScan & isNumberModeActive==false & numberModeToggle==0 & isspecialCharMode==false & isAutoSuggestionMode==true & autoSuggestionModeToggle ==1 & data==2) // Forward Navigation in AutoSuggestion Mode
            {
                autoSuggestionsMode.forwardNavigateSuggestions(tts,SuggestionsResult);

                //countTotalTaps.performCounting("autoSuggestionModeForwardNav");
            }*/
            else if(isAutoSuggestionMode==true & data==14)//Selection in AutoSuggestion Mode
            {
                System.out.println("Auto suggestion mode selection");
                String word = autoSuggestionsMode.selectAutoSuggestion(tts, autoSuggestionCounter);

                if(retAlphabetString.contains(" "))
                {
                    retAlphabetString  = "";

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
                    retAlphabetString  = temp+" "+word;
                }
                else
                {
                    retAlphabetString  = word;
                }

                Log.d("TypedString",retAlphabetString );

                //countTotalTaps.performCounting("autoSuggestionModeSelection");
            }
        }

        @Override
        public synchronized void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {

            Log.i("mkonMouseInputReceived",tapIdentifier);
            Log.d("mkMYINT","data"+data);
            Log.d("mkmousedx","data"+data.dx.getInt());
            Log.d("mkmousedy","data"+data.dy.getInt());
            Log.d("mkmousedt","data"+data.dt.getInt());
            Log.d("mkmouseproximity","data"+data.proximity.getInt());
            //log(tapIdentifier+"mkmouseinputreceived"+data.dx.getInt()+","+data.dy.getInt());
            //log(tapIdentifier+"mkmouseinputreceived"+data.dx.getInt()+","+data.dy.getInt());

            if (data.dy.getInt() > 1 ){ //forward scroll
                inScrollMode = true;
                //tts.speak(" Alphabets scroll mode", TextToSpeech.QUEUE_FLUSH, null, null);

                if (isAlphabetModeActive == true) {
                    Log.d("indexfinger forward scroll - scroll through alphabets", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Falphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if(alphabetCounter > 18){
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
                            tts.setSpeechRate(2.0f);
                            tts.speak("space",TextToSpeech.QUEUE_FLUSH,null,null);
                            alphabetIndex = 0;//reset the position
                        }
                        else {
                            Log.d("Falphabet index value", "alphabetIndex" + alphabetIndex);
                            String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                            glAlphabetString = currentAlphabet;
                            System.out.println("FScrolling to: " + currentAlphabet);
                            System.out.println("FglAlphabetString: " + glAlphabetString);
                            tts.setSpeechRate(2.0f);
                            tts.speak(currentAlphabet, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    else {
                        Log.i("Fmk do nothing",tapIdentifier);
                    }
                }
                else if(isNumberModeActive == true){
                    Log.d("indexfinger forward scroll - scroll through numbers", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Fnumber counter val ", "alphabetCounter" + alphabetCounter);
                    if(alphabetCounter > 18){
                        alphabetCounter = 0;
                        numberIndex++;
                        if (numberIndex < 0) {
                            numberIndex = 0;
                        }
                        else if (numberIndex >= number.length) {
                            numberIndex = number.length - 1;
                        }
                        else if (numberIndex == 10){
                            numberIndex = 0;
                        }

                        Log.d("Fnumber index value", "numberIndex" + numberIndex);
                        String currentNumber = String.valueOf(number[numberIndex]);
                        glAlphabetString = currentNumber;
                        System.out.println("FScrolling to: " + currentNumber);
                        System.out.println("FglNumberString: " + glAlphabetString);
                        tts.speak(currentNumber, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                else if (isAutoSuggestionMode==true) {
                    alphabetCounter++;

                    Log.d("Auto Suggestions Mode", "data" + data.dy.getInt());
                    //autoSuggestionsMode.forwardNavigateSuggestions(tts,SuggestionsResult);
                    if(alphabetCounter > 10){
                        autoSuggestionCounter++;
                        alphabetCounter = 0;

                        if (autoSuggestionCounter>=SuggestionsResult.size()){
                            autoSuggestionCounter=0;
                        }
                        else {
                            tts.speak(SuggestionsResult.get(autoSuggestionCounter),TextToSpeech.QUEUE_ADD,null,null);
                        }
                    }
                }
                else if(isspecialCharMode == true){
                    alphabetCounter++;

                    Log.d("Special character countSpecial character count", "data" + data.dy.getInt());
                    if(alphabetCounter > 5){
                        alphabetCounter = 0;
                        specialCharacterCounter++;
                        if (specialCharacterCounter < 0) {
                            specialCharacterCounter = 0;
                        }
                        else if (specialCharacterCounter >= specialCharacters.length) {
                            specialCharacterCounter = specialCharacters.length - 1;
                        }
                        else if (specialCharacterCounter == 8){
                            specialCharacterCounter = 0;
                        }

                        Log.d("special character index value", "specialCharacterCounter" + specialCharacterCounter);
                        String currentSpecialCharacter = String.valueOf(specialCharacters[specialCharacterCounter]);
                        glAlphabetString = currentSpecialCharacter;
                        System.out.println("FScrolling to: " + currentSpecialCharacter);
                        System.out.println("FglNumberString: " + glAlphabetString);

                        if(glAlphabetString.equals("#"))
                        {
                            tts.speak("Hash",TextToSpeech.QUEUE_FLUSH,null,null);
                        }
                        else if(glAlphabetString.equals("$"))
                        {
                            tts.speak("Dollar",TextToSpeech.QUEUE_FLUSH,null,null);
                        }
                        else {
                            tts.speak(currentSpecialCharacter, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    else {
                        System.out.println("special character didn't enter loop");
                    }
                }
                else if(isEditModeReplaceActive==true){
                    if (editModeIndex < 0 || editModeIndex >= editStringLength) {
                        editModeIndex = 0;
                    }
                    Log.d("currentEditCharacter ", "editIndex" + editModeIndex);
                    Character currentEditCharacter = retAlphabetString.charAt(editModeIndex);
                    tts.speak(currentEditCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                    editModeIndexBuffer = editModeIndex;
                    editModeIndex++;
                    Log.d("Edit Counter ", "editIndex" + editModeIndex);
                }
            }
            else if (data.dy.getInt() < -1){ //backward scroll
                inScrollMode = true;
                if (isAlphabetModeActive == true) {
                    Log.d("indexfinger backward scroll - scroll through alphabets", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Balphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 18) {
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
                else if (isNumberModeActive == true) {
                    Log.d("indexfinger backward scroll - scroll through numbers", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Balphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 18) {
                        alphabetCounter = 0;
                        numberIndex--;
                        if (numberIndex == 0) {
                            numberIndex = 10;
                        } else if (numberIndex >= number.length) {
                            numberIndex = number.length - 1;
                        }

                        Log.d("Bnumber index value", "numberIndex" + numberIndex);
                        String currentNumber = String.valueOf(number[numberIndex]);
                        glAlphabetString = currentNumber;
                        System.out.println("BScrolling to: " + currentNumber);
                        System.out.println("BglNumberString: " + glAlphabetString);
                        tts.speak(currentNumber, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                     else {
                        Log.i("Bmk do nothing", tapIdentifier);
                    }
                }
                else if (isAutoSuggestionMode==true) {
                    Log.d("Auto Suggestions Mode", "data" + data.dy.getInt());


                    alphabetCounter++;

                    Log.d("Auto Suggestions Mode", "data" + data.dy.getInt());
                    //autoSuggestionsMode.forwardNavigateSuggestions(tts,SuggestionsResult);
                    if(alphabetCounter > 10){
                        autoSuggestionCounter--;
                        alphabetCounter = 0;

                        if (autoSuggestionCounter>=SuggestionsResult.size()){
                            autoSuggestionCounter=0;
                        }
                        else {
                            tts.speak(SuggestionsResult.get(autoSuggestionCounter),TextToSpeech.QUEUE_ADD,null,null);
                        }
                    }
                }
                else if(isspecialCharMode == true){
                    alphabetCounter++;

                    Log.d("Special character countSpecial character count", "data" + data.dy.getInt());
                    if(alphabetCounter > 15){
                        alphabetCounter = 0;
                        specialCharacterCounter++;
                        if (specialCharacterCounter < 0) {
                            specialCharacterCounter = 0;
                        }
                        else if (specialCharacterCounter >= specialCharacters.length) {
                            specialCharacterCounter = specialCharacters.length - 1;
                        }
                        else if (specialCharacterCounter == 8){
                            specialCharacterCounter = 0;
                        }

                        Log.d("special character index value", "specialCharacterCounter" + specialCharacterCounter);
                        String currentSpecialCharacter = String.valueOf(specialCharacters[specialCharacterCounter]);
                        glAlphabetString = currentSpecialCharacter;
                        System.out.println("FScrolling to: " + currentSpecialCharacter);
                        System.out.println("FglNumberString: " + glAlphabetString);

                        if(glAlphabetString.equals("#"))
                        {
                            tts.speak("Hash",TextToSpeech.QUEUE_FLUSH,null,null);
                        }
                        else if(glAlphabetString.equals("$"))
                        {
                            tts.speak("Dollar",TextToSpeech.QUEUE_FLUSH,null,null);
                        }
                        else
                        {
                            tts.speak(currentSpecialCharacter, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                    else {
                        System.out.println("special character didn't enter loop");
                    }
                }
                else if(isEditModeReplaceActive == true) {
                    alphabetCounter++;
                    if(alphabetCounter > 15){
                        alphabetCounter = 0;
                        if (editModeIndex < 0 || editModeIndex >= editStringLength) {
                            editModeIndex = editStringLength-1;
                        }
                        Character currentEditCharacter = retAlphabetString.charAt(editModeIndex);
                        tts.speak(currentEditCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                        editModeIndexBuffer = editModeIndex;
                        editModeIndex--;
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
