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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 1234;
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

    private String alphabetString = " abcdefghijklmnopqrstuvwxyz ";
    private char[] number = " 0123456789 ".toCharArray();

    private char[] specialCharacters = " &.@!*#$%? ".toCharArray();
    int alphabetIndex = 0;

    int chunkAlphabetIndex = 0;
    int numberIndex = 0;

    int globalEditIndex = 0;

    int deleteModeIndex = 0;

    private int alphabetCounter = 0;
    private int numberCounter = 0;
    //Auto Suggestion counter
    public int autoSuggestionCounter = 0;

    private int specialCharacterCounter = 0;
    private boolean selectingAlphabet = false;
    private String selectedAlphabet = null;

    String glAlphabetString = "";
    String retAlphabetString = "";

    static boolean isAlphabetModeActive = false;
    static boolean isNumberModeActive = false;

    //Variables for edit Mode
    static boolean isEditModeReplaceActive = false;
    static boolean isEditModeInsertActive = false;

    static boolean isDeletionModeActive = false;
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

            /*Speak out once tap strap is connected to phone*/
            alMode.speakOut(tts,"Tap Strap connected to the phone. You can start keyflow");


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


        @Override
        public synchronized void onTapInputReceived(@NonNull String tapIdentifier, int data, int repetetion) {
            EarconManager earconManager = new EarconManager();
            earconManager.setupEarcons(MainActivity.tts,getApplicationContext().getPackageName());

            Log.d("onTapInputReceived","data"+data);

            /*
             *  ###########Alphabet Mode Coding Starts Here################
             * */

            // alMode -> Forward
            if(!allowSearchScan & isAlphabetModeActive == false  & data==2)
            {
                tts.speak("Entered Alphabet Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                isAlphabetModeActive = true;
                isNumberModeActive = false;
                isAutoSuggestionMode = false;
                isspecialCharMode = false;
                isDeletionModeActive = false;
            }
            else if(isAlphabetModeActive == true & data == 2){
                Log.d("Alphabet selected ","data"+data);


                if(isEditModeReplaceActive == false & isEditModeInsertActive == false){
                    retAlphabetString += glAlphabetString;
                    Log.d("Alphabet isEditModeReplaceActive-false ","retAlphabetString"+retAlphabetString);
                }
                /*else if(isEditModeReplaceActive==true & isEditModeInsertActive == false& !retAlphabetString.isEmpty()){
                    char[] charArray  = retAlphabetString.toCharArray();
                    System.out.println("isEditModeReplaceActive true - charArray: " + Arrays.toString(charArray));
                    System.out.println("isEditModeReplaceActive true - glAlphabetString 0: " + glAlphabetString.charAt(0));
                    System.out.println("isEditModeReplaceActive true - charArray editModeIndexBuffer: " + charArray[editModeIndexBuffer]);
                    charArray[editModeIndexBuffer] = glAlphabetString.charAt(0);
                    retAlphabetString = new String(charArray);
                    isAlphabetModeActive = false;
                }*/
                else if(isEditModeReplaceActive==false & isEditModeInsertActive == true& !retAlphabetString.isEmpty()){
                    System.out.println("isEditModeInsertActive true - glAlphabetString 0: " + glAlphabetString.charAt(0));
                    System.out.println("isEditModeInsertActive true - editModeIndexBuffer  " + editModeIndexBuffer);

                    StringBuilder sb = new StringBuilder(retAlphabetString);
                    sb.insert(++editModeIndexBuffer,glAlphabetString.charAt(0));
                    retAlphabetString = sb.toString();
                    Log.i("isEditModeInsertActive Word",retAlphabetString);
                    tts.speak(retAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                    //isAlphabetModeActive = false;

                    //tts.setPitch(1.5f);
                    //if (retAlphabetString.length()==0) {
                      //  tts.speak("No message typed", TextToSpeech.QUEUE_FLUSH, null, null);
                    //}
                    //else
                    //{
                    tts.setPitch(0.5f);
                        System.out.println("SpeakOutRetAlphabetString ");
                        tts.speak(retAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                    //}
                    System.out.println("Exited EditInsert mode ");
                    tts.speak("Exited Edit insert mode", TextToSpeech.QUEUE_FLUSH, null, null);
                    //tts.setPitch(1.0f);

                    isEditModeInsertActive = false;
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

            }
            // alMode -> SpeakOut
            else if(data ==30)
            {
                System.out.println("retAlphabetString: data is " + data);
                System.out.println("retAlphabetString: " + retAlphabetString);
                tts.setPitch(1.5f);
                if (retAlphabetString.length()==0) {
                    tts.speak("No message typed", TextToSpeech.QUEUE_FLUSH, null, null);
                }
                else
                {
                    tts.speak(retAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                }
                tts.setPitch(1.0f);
            }
            else if(data == 6)// alMode chunking
            {
                System.out.println("retAlphabetString: data is " + data);
                System.out.println("retAlphabetString: " + retAlphabetString);

                if (chunkAlphabetIndex == 0){
                    alphabetIndex = 1;//a
                }
                else if (chunkAlphabetIndex == 1){
                    alphabetIndex = 5;//e
                }
                else if (chunkAlphabetIndex == 2){
                    alphabetIndex = 9;//i
                }
                else if (chunkAlphabetIndex == 3){
                    alphabetIndex = 15;//o
                }
                else if (chunkAlphabetIndex == 4){
                    alphabetIndex = 21;//u
                }
                else{
                    System.out.println("chunkingAlphaINdex 1");
                    alphabetIndex = 1;
                    chunkAlphabetIndex = 1;
                }

                Log.d("Falphabet index value", "alphabetIndex" + alphabetIndex);
                String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                glAlphabetString = currentAlphabet;
                System.out.println("FScrolling to: " + currentAlphabet);
                System.out.println("FglAlphabetString: " + glAlphabetString);
                tts.setSpeechRate(1.5f);
                tts.speak(currentAlphabet, TextToSpeech.QUEUE_FLUSH, null, null);

                chunkAlphabetIndex++;

                Log.d("chunkAlphabetIndex  value", "chunkAlphabetIndex" + chunkAlphabetIndex);
            }
            // alMode ->Deletion
            else if(isDeletionModeActive == false & (data == 24 | data == 16))
            {
                Log.d("Delete mode entered","data"+data);

                isDeletionModeActive = true;

                isNumberModeActive = false;

                isAlphabetModeActive = false;

                isAutoSuggestionMode=false;

                isspecialCharMode = false;

                String deleted_alphabet = String.valueOf(retAlphabetString.charAt(retAlphabetString.length() - 1));
                System.out.println(" Deletion retAlphabetString: " + retAlphabetString);
                //tts.speak(deleted_alphabet,TextToSpeech.QUEUE_ADD,null,null);
                tts.speak("delete at "+ deleted_alphabet.toString(), TextToSpeech.QUEUE_ADD, null, null);

                deleteModeIndex = retAlphabetString.length() - 1;
                //retAlphabetString = retAlphabetString.substring(0, retAlphabetString.length() - 1);
                //tts.playEarcon(deleteChar,TextToSpeech.QUEUE_FLUSH,null,null);

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
            else if(isDeletionModeActive == true & (data == 24 | data == 16)){
                Log.d("Delete mode entered isDeletionModeActive true","data"+data);

                if (deleteModeIndex == retAlphabetString.length() -1)  {
                    retAlphabetString = retAlphabetString.substring(0, deleteModeIndex);
                }
                else {
                    retAlphabetString = retAlphabetString.substring(0, deleteModeIndex) + retAlphabetString.substring(deleteModeIndex); //test this
                }
                ;//check if this works for deletion of string in the middle
                tts.playEarcon(deleteChar,TextToSpeech.QUEUE_FLUSH,null,null);

                System.out.println(" Deletion retAlphabetString: " + retAlphabetString);
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

                isDeletionModeActive = false;
            }
            else if(isNumberModeActive == true & data == 4){
                selectingAlphabet = true;
                Log.d("Number  selected ","data"+data);
                System.out.println("isEditModeReplaceActive val: " + isEditModeReplaceActive);

                if(isEditModeReplaceActive == false & isEditModeInsertActive == false){
                    System.out.println("isEditModeReplaceActive false: " + glAlphabetString);
                    retAlphabetString += glAlphabetString;
                }
                /*else if(isEditModeReplaceActive==true & isEditModeInsertActive == false & !retAlphabetString.isEmpty()){
                    System.out.println("isEditModeReplaceActive true: " + glAlphabetString);
                    char[] charArray = retAlphabetString.toCharArray();
                    charArray[editModeIndexBuffer] = glAlphabetString.charAt(0);
                    retAlphabetString = new String(charArray);
                }*/
                else if(isEditModeReplaceActive==false & isEditModeInsertActive == true& !retAlphabetString.isEmpty()){
                    System.out.println("isEditModeInsertActive true - glAlphabetString 0: " + glAlphabetString.charAt(0));
                    System.out.println("isEditModeInsertActive true - editModeIndexBuffer  " + editModeIndexBuffer);

                    StringBuilder sb = new StringBuilder(retAlphabetString);
                    sb.insert(++editModeIndexBuffer,glAlphabetString.charAt(0));
                    retAlphabetString = sb.toString();
                    Log.i("isEditModeInsertActive Word",retAlphabetString);
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
                isNumberModeActive = false;
                isAutoSuggestionMode = false;
                isDeletionModeActive = false;
            }
            else if(isspecialCharMode == true & data == 8){
                Log.d("Special Character selected ","data"+data);

                if(isEditModeReplaceActive == false & isEditModeInsertActive == false){
                    retAlphabetString += glAlphabetString;
                }
                /*else if(isEditModeReplaceActive==true & isEditModeInsertActive == false & !retAlphabetString.isEmpty()){
                    char[] charArray = retAlphabetString.toCharArray();
                    charArray[editModeIndexBuffer] = glAlphabetString.charAt(0);
                    retAlphabetString = new String(charArray);
                }*/
                else if(isEditModeReplaceActive==false & isEditModeInsertActive == true& !retAlphabetString.isEmpty()){
                    System.out.println("isEditModeInsertActive true - glAlphabetString 0: " + glAlphabetString.charAt(0));
                    System.out.println("isEditModeInsertActive true - editModeIndexBuffer  " + editModeIndexBuffer);

                    StringBuilder sb = new StringBuilder(retAlphabetString);
                    sb.insert(++editModeIndexBuffer,glAlphabetString.charAt(0));
                    retAlphabetString = sb.toString();
                    Log.i("isEditModeInsertActive Word",retAlphabetString);
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
                else if(glAlphabetString.equals(" ") ) {
                    tts.setPitch(1.5f);
                    tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                    Log.d("selection speak out space ", "alphabetIndex" + alphabetIndex);
                    tts.setPitch(1.0f);
                }
                else
                {
                    System.out.println("glAlphabetString is not space " + glAlphabetString);
                    tts.setPitch(1.5f);
                    tts.speak(glAlphabetString, TextToSpeech.QUEUE_FLUSH, null, null);
                    tts.setPitch(1.0f);
                }
            }
            /*else if(isEditModeReplaceActive == false & data == 6){
                tts.speak("Entered Edit Replace Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Entered Edit Replace Mode","data"+data);

                isEditModeReplaceActive=true;
                isEditModeInsertActive=false;
                isspecialCharMode=false;
                isAlphabetModeActive = false;
                isNumberModeActive = false;
                isAutoSuggestionMode = false;
                isDeletionModeActive = false;
                editStringLength = retAlphabetString.length();
            }
            else if(isEditModeReplaceActive == true & data == 6){
                isEditModeReplaceActive=false;*//*TODO: Exit mode should automatically happen when user sets to a different mode. */
                /*tts.speak("Exit Edit Replace Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Exit Edit Replace Mode","data"+data);
            }*/
            else if(isEditModeInsertActive == false & data == 12){
                tts.speak("Entered Edit Insert Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Entered Edit Insert Mode","data"+data);

                isEditModeReplaceActive=false;
                isEditModeInsertActive=true;
                isspecialCharMode=false;
                isAlphabetModeActive = false;
                isNumberModeActive = false;
                isAutoSuggestionMode = false;
                isDeletionModeActive = false;
                editStringLength = retAlphabetString.length();
            }
            /*
            else if(isEditModeInsertActive == true & data == 12){
                isEditModeInsertActive=false;
                tts.speak("Exit Edit Insert Mode",TextToSpeech.QUEUE_FLUSH,null,null);
                Log.d("Exit Edit Insert Mode","data"+data);
            }
            */

            /*
             *   #####Autosuggestions Mode
             * */

            else if(isAutoSuggestionMode==false & data==14)
            {
                isspecialCharMode=false;
                isAlphabetModeActive = false;
                isNumberModeActive = false;
                isAutoSuggestionMode = true;
                isDeletionModeActive = false;

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
            }
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
            }
        }

        @Override
        public synchronized void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {

            Log.d("mkMYINT", "data" + data);
            Log.d("mkmousedx", "data" + data.dx.getInt());
            Log.d("mkmousedy", "data" + data.dy.getInt());
            Log.d("mkmouseproximity", "data" + data.proximity.getInt());

            if (data.dy.getInt() > 1) { //forward scroll
                inScrollMode = true;

                if (isAlphabetModeActive == true) {
                    Log.d("indexfinger forward scroll - scroll through alphabets", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Falphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 18) {
                        if (isEditModeReplaceActive == true) {
                            alphabetCounter = 0;
                            globalEditIndex++;
                            if (globalEditIndex < 0) {
                                globalEditIndex = 0;
                            } else if (globalEditIndex >= alphabet.length) {
                                globalEditIndex = alphabet.length - 1;
                            }
                            if (globalEditIndex == 27) {
                                Log.d("Fspeak out space ", "alphabetIndex" + globalEditIndex);
                                String currentAlphabet = String.valueOf(alphabet[globalEditIndex]);
                                glAlphabetString = currentAlphabet;
                                System.out.println("FScrolling to: " + currentAlphabet);
                                tts.setSpeechRate(1.5f);
                                tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                                alphabetIndex = 0;//reset the position
                            } else {
                                Log.d("Falphabet index value", "alphabetIndex" + globalEditIndex);
                                String currentAlphabet = String.valueOf(alphabet[globalEditIndex]);
                                glAlphabetString = currentAlphabet;
                                System.out.println("FScrolling to: " + currentAlphabet);
                                System.out.println("FglAlphabetString: " + glAlphabetString);
                                tts.setSpeechRate(1.5f);
                                tts.speak(currentAlphabet, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        }
                        else{
                            alphabetCounter = 0;
                            alphabetIndex++;
                            if (alphabetIndex < 0) {
                                alphabetIndex = 0;
                            } else if (alphabetIndex >= alphabet.length) {
                                alphabetIndex = alphabet.length - 1;
                            }
                            if (alphabetIndex == 27) {
                                Log.d("Fspeak out space ", "alphabetIndex" + alphabetIndex);
                                String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                                glAlphabetString = currentAlphabet;
                                System.out.println("FScrolling to: " + currentAlphabet);
                                tts.setSpeechRate(1.5f);
                                tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                                alphabetIndex = 0;//reset the position
                            } else {
                                Log.d("Falphabet index value", "alphabetIndex" + alphabetIndex);
                                String currentAlphabet = String.valueOf(alphabet[alphabetIndex]);
                                glAlphabetString = currentAlphabet;
                                System.out.println("FScrolling to: " + currentAlphabet);
                                System.out.println("FglAlphabetString: " + glAlphabetString);
                                tts.setSpeechRate(1.5f);
                                tts.speak(currentAlphabet, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        }
                    } else {
                        Log.i("Fmk do nothing", tapIdentifier);
                    }
                } else if (isNumberModeActive == true) {
                    Log.d("indexfinger forward scroll - scroll through numbers", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Fnumber counter val ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 18) {
                        alphabetCounter = 0;
                        numberIndex++;
                        if (numberIndex < 0) {
                            numberIndex = 0;
                        } else if (numberIndex >= number.length) {
                            numberIndex = number.length - 1;
                        }

                        if (numberIndex == 11 || numberIndex == 0) {
                            Log.d("Fspeak out space ", "alphabetIndex" + alphabetIndex);
                            String currentNumber = String.valueOf(number[numberIndex]);
                            glAlphabetString = currentNumber;
                            System.out.println("FScrolling to: " + currentNumber);
                            tts.setSpeechRate(1.5f);
                            tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                            numberIndex = 0;//reset the position
                        }
                        else {
                            Log.d("Fnumber index value", "numberIndex" + numberIndex);
                            String currentNumber = String.valueOf(number[numberIndex]);
                            glAlphabetString = currentNumber;
                            System.out.println("FScrolling to: " + currentNumber);
                            System.out.println("FglNumberString: " + glAlphabetString);
                            tts.speak(currentNumber, TextToSpeech.QUEUE_FLUSH, null, null);
                        }


                    }
                } else if (isAutoSuggestionMode == true) {
                    alphabetCounter++;

                    Log.d("Auto Suggestions Mode", "data" + data.dy.getInt());
                    //autoSuggestionsMode.forwardNavigateSuggestions(tts,SuggestionsResult);
                    if (alphabetCounter > 16) {
                        autoSuggestionCounter++;
                        alphabetCounter = 0;

                        if (autoSuggestionCounter >= SuggestionsResult.size()) {
                            autoSuggestionCounter = 0;
                        } else {
                            tts.speak(SuggestionsResult.get(autoSuggestionCounter), TextToSpeech.QUEUE_ADD, null, null);
                        }
                    }
                } else if (isspecialCharMode == true) {
                    alphabetCounter++;

                    Log.d("Special character countSpecial character count", "data" + data.dy.getInt());
                    if (alphabetCounter > 5) {
                        alphabetCounter = 0;
                        specialCharacterCounter++;
                        if (specialCharacterCounter < 0) {
                            Log.d("special character 0", "specialCharacterCounter" + specialCharacterCounter);
                            specialCharacterCounter = 0;
                        } else if (specialCharacterCounter >= specialCharacters.length) {
                            specialCharacterCounter = specialCharacters.length - 1;
                        }


                        Log.d("special character index value", "specialCharacterCounter" + specialCharacterCounter);
                        String currentSpecialCharacter = String.valueOf(specialCharacters[specialCharacterCounter]);
                        glAlphabetString = currentSpecialCharacter;
                        System.out.println("FScrolling to: " + currentSpecialCharacter);
                        System.out.println("FglNumberString: " + glAlphabetString);

                        if (glAlphabetString.equals("#")) {
                            tts.speak("Hash", TextToSpeech.QUEUE_FLUSH, null, null);
                        } else if (glAlphabetString.equals("$")) {
                            tts.speak("Dollar", TextToSpeech.QUEUE_FLUSH, null, null);
                        } else if (glAlphabetString.equals(" ")){
                            tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                            specialCharacterCounter = 0;
                        } else {
                            tts.speak(currentSpecialCharacter, TextToSpeech.QUEUE_FLUSH, null, null);
                        }

                    } else {
                        System.out.println("special character didn't enter loop");
                    }
                } /*else if (isEditModeReplaceActive == true) {
                    alphabetCounter++;
                    Log.d("editIndex ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 20) {
                        alphabetCounter = 0;
                        if (editModeIndex < 0 || editModeIndex >= editStringLength) {
                            editModeIndex = 0;
                        }
                        Log.d("editIndex inside if", "alphabetCounter" + alphabetCounter);
                        Log.d("editIndex ", "editIndex" + editModeIndex);
                        Character currentEditCharacter = retAlphabetString.charAt(editModeIndex);
                        Log.d("currentEditCharacter ", "currentEditCharacter" + currentEditCharacter);
                        tts.setSpeechRate(1.5f);
                        if (Character.isWhitespace(currentEditCharacter)) {
                            tts.speak("Replace at space", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        tts.speak(" Replace at " + currentEditCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                        globalEditIndex = alphabetString.indexOf(currentEditCharacter.toString());
                        Log.d("globalEditIndex ", "globalEditIndex" + globalEditIndex);
                        editModeIndexBuffer = editModeIndex;
                        editModeIndex++;
                        Log.d("Edit Counter ", "editIndex" + editModeIndex);
                    }
                }*/ else if (isEditModeInsertActive == true) {
                    alphabetCounter++;
                    Log.d("editIndex ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 20) {
                        alphabetCounter = 0;
                        if (editModeIndex < 0 || editModeIndex >= editStringLength) {
                            editModeIndex = 0;
                        }
                        Log.d("editIndex inside if", "alphabetCounter" + alphabetCounter);
                        Log.d("editIndex ", "editIndex" + editModeIndex);
                        Character currentEditCharacter = retAlphabetString.charAt(editModeIndex);
                        Log.d("currentEditCharacter ", "currentEditCharacter" + currentEditCharacter);
                        tts.setSpeechRate(1.5f);
                        if (Character.isWhitespace(currentEditCharacter)) {
                            tts.speak("Insert after space", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        tts.speak(" Insert after" + currentEditCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                        globalEditIndex = alphabetString.indexOf(currentEditCharacter.toString());
                        editModeIndexBuffer = editModeIndex;
                        editModeIndex++;
                        Log.d("Edit Counter ", "editIndex" + editModeIndex);
                    }
                }
                else if  (isDeletionModeActive == true){
                    alphabetCounter++;
                    //deleteModeIndex = retAlphabetString.length() - 1;
                    Log.d("editIndex ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 20) {
                        alphabetCounter = 0;
                        if (deleteModeIndex < 0 || deleteModeIndex >= retAlphabetString.length() - 1 ) {
                            deleteModeIndex = retAlphabetString.length() - 1;
                        }

                        deleteModeIndex--;
                        Log.d("isDeletionModeActive scroll true", "alphabetCounter" + alphabetCounter);
                        Log.d("deleteModeIndex ", "deleteModeIndex" + deleteModeIndex);
                        Character currentDeleteCharacter = retAlphabetString.charAt(deleteModeIndex);
                        Log.d("currentDeleteCharacter ", "currentDeleteCharacter" + currentDeleteCharacter);
                        tts.setSpeechRate(1.5f);
                        if (currentDeleteCharacter.equals(" ")){
                            tts.speak("Delete space", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        tts.speak(" Delete " + currentDeleteCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                        Log.d("isDeletionModeActive ", "deleteModeIndex" + deleteModeIndex);
                    }
                }
            } else if (data.dy.getInt() < -1) { //backward scroll
                inScrollMode = true;

                if (isAlphabetModeActive == true) {
                    Log.d("indexfinger backward scroll - scroll through alphabets", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Balphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 18) {
                        alphabetCounter = 0;
                        if (isEditModeReplaceActive == true){
                            globalEditIndex--;
                            if (globalEditIndex < 0) {
                                globalEditIndex = 0;
                            } else if (globalEditIndex >= alphabet.length) {
                                globalEditIndex = alphabet.length - 1;
                            }
                            if (globalEditIndex == 27) {
                                Log.d("Fspeak out space ", "alphabetIndex" + globalEditIndex);
                                String currentAlphabet = String.valueOf(alphabet[globalEditIndex]);
                                glAlphabetString = currentAlphabet;
                                System.out.println("FScrolling to: " + currentAlphabet);
                                tts.setSpeechRate(1.5f);
                                tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                                alphabetIndex = 0;//reset the position
                            } else {
                                Log.d("Falphabet index value", "alphabetIndex" + globalEditIndex);
                                String currentAlphabet = String.valueOf(alphabet[globalEditIndex]);
                                glAlphabetString = currentAlphabet;
                                System.out.println("FScrolling to: " + currentAlphabet);
                                System.out.println("FglAlphabetString: " + glAlphabetString);
                                tts.setSpeechRate(1.5f);
                                tts.speak(currentAlphabet, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        }
                        alphabetIndex--;
                        if (alphabetIndex == 0) {
                            alphabetIndex = 27;
                        } else if (alphabetIndex >= alphabet.length) {
                            alphabetIndex = alphabet.length - 1;
                        }

                        if (alphabetIndex == 27) {
                            Log.d("Bspeak out space ", "alphabetIndex" + alphabetIndex);
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
                } else if (isNumberModeActive == true) {
                    Log.d("indexfinger backward scroll - scroll through numbers", "data" + data.dy.getInt());

                    alphabetCounter++;
                    Log.d("Balphabet counter val ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 18) {
                        alphabetCounter = 0;
                        numberIndex--;
                        if (numberIndex == 0) {
                            //numberIndex = 10;
                            System.out.println("numberIndex 0 ");
                        } else if (numberIndex >= number.length) {
                            numberIndex = number.length - 1;
                        }

                        if (numberIndex == 11 || numberIndex == 0) {
                            Log.d("Fspeak out space ", "alphabetIndex" + alphabetIndex);
                            String currentNumber = String.valueOf(number[numberIndex]);
                            glAlphabetString = currentNumber;
                            System.out.println("FScrolling to: " + currentNumber);
                            tts.setSpeechRate(1.5f);
                            tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                            numberIndex = 0;//reset the position
                        }
                        else{
                            Log.d("Bnumber index value", "numberIndex" + numberIndex);
                            String currentNumber = String.valueOf(number[numberIndex]);
                            glAlphabetString = currentNumber;
                            System.out.println("BScrolling to: " + currentNumber);
                            System.out.println("BglNumberString: " + glAlphabetString);
                            tts.speak(currentNumber, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    } else {
                        Log.i("Bmk do nothing", tapIdentifier);
                    }
                } else if (isAutoSuggestionMode == true) {
                    Log.d("Auto Suggestions Mode", "data" + data.dy.getInt());


                    alphabetCounter++;

                    Log.d("Auto Suggestions Mode", "data" + data.dy.getInt());
                    if (alphabetCounter > 16) {
                        autoSuggestionCounter--;
                        alphabetCounter = 0;

                        if (autoSuggestionCounter >= SuggestionsResult.size()) {
                            autoSuggestionCounter = 0;
                        } else {
                            tts.speak(SuggestionsResult.get(autoSuggestionCounter), TextToSpeech.QUEUE_ADD, null, null);
                        }
                    }
                } else if (isspecialCharMode == true) {
                    alphabetCounter++;

                    Log.d("Special character countSpecial character count", "data" + data.dy.getInt());
                    if (alphabetCounter > 15) {
                        alphabetCounter = 0;
                        specialCharacterCounter++;
                        if (specialCharacterCounter < 0) {
                            specialCharacterCounter = 0;
                        } else if (specialCharacterCounter >= specialCharacters.length) {
                            specialCharacterCounter = specialCharacters.length - 1;
                        } else if (specialCharacterCounter == 8) {
                            specialCharacterCounter = 0;
                        }

                        Log.d("special character index value", "specialCharacterCounter" + specialCharacterCounter);
                        String currentSpecialCharacter = String.valueOf(specialCharacters[specialCharacterCounter]);
                        glAlphabetString = currentSpecialCharacter;
                        System.out.println("FScrolling to: " + currentSpecialCharacter);
                        System.out.println("FglNumberString: " + glAlphabetString);

                        if (glAlphabetString.equals("#")) {
                            tts.speak("Hash", TextToSpeech.QUEUE_FLUSH, null, null);
                        } else if (glAlphabetString.equals("$")) {
                            tts.speak("Dollar", TextToSpeech.QUEUE_FLUSH, null, null);
                        } else if (glAlphabetString.equals(" ")){
                            tts.speak("space", TextToSpeech.QUEUE_FLUSH, null, null);
                            specialCharacterCounter = 0;
                        } else {
                            tts.speak(currentSpecialCharacter, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    } else {
                        System.out.println("special character didn't enter loop");
                    }
                } /*else if (isEditModeReplaceActive == true) {
                    alphabetCounter++;
                    Log.d("editIndex ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 20) {
                        alphabetCounter = 0;
                        if (editModeIndex < 0 || editModeIndex >= editStringLength) {
                            editModeIndex = 0;
                        }
                        Log.d("editIndex inside if", "alphabetCounter" + alphabetCounter);
                        Log.d("editIndex ", "editIndex" + editModeIndex);
                        Character currentEditCharacter = retAlphabetString.charAt(editModeIndex);
                        Log.d("currentEditCharacter ", "currentEditCharacter" + currentEditCharacter);
                        tts.setSpeechRate(1.5f);
                        if (Character.isWhitespace(currentEditCharacter)) {
                            tts.speak("Replace at space", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        tts.speak(" Replace at " + currentEditCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                        editModeIndexBuffer = editModeIndex;
                        editModeIndex++;
                        Log.d("Edit Counter ", "editIndex" + editModeIndex);
                    }
                }*/ else if (isEditModeInsertActive == true) {
                    alphabetCounter++;
                    Log.d("editIndex ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 20) {
                        alphabetCounter = 0;
                        if (editModeIndex < 0 || editModeIndex >= editStringLength) {
                            editModeIndex = 0;
                        }
                        Log.d("editIndex inside if", "alphabetCounter" + alphabetCounter);
                        Log.d("editIndex ", "editIndex" + editModeIndex);
                        Character currentEditCharacter = retAlphabetString.charAt(editModeIndex);
                        Log.d("currentEditCharacter ", "currentEditCharacter" + currentEditCharacter);
                        tts.setSpeechRate(1.5f);
                        if (Character.isWhitespace(currentEditCharacter)) {
                            tts.speak("Insert after space", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        tts.speak(" Insert after" + currentEditCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                        editModeIndexBuffer = editModeIndex;
                        editModeIndex++;
                        Log.d("Edit Counter ", "editIndex" + editModeIndex);
                    }
                }
                else if  (isDeletionModeActive == true){
                    alphabetCounter++;
                    //deleteModeIndex = retAlphabetString.length();
                    Log.d("editIndex ", "alphabetCounter" + alphabetCounter);
                    if (alphabetCounter > 20) {
                        alphabetCounter = 0;
                        if (deleteModeIndex < 0 || deleteModeIndex >= retAlphabetString.length() - 1) {
                            deleteModeIndex = 0;
                        }

                        deleteModeIndex++;
                        Log.d("isDeletionModeActive scroll true", "alphabetCounter" + alphabetCounter);
                        Log.d("deleteModeIndex ", "deleteModeIndex" + deleteModeIndex);
                        Character currentDeleteCharacter = retAlphabetString.charAt(deleteModeIndex - 1);
                        Log.d("currentDeleteCharacter ", "currentDeleteCharacter" + currentDeleteCharacter);
                        tts.setSpeechRate(1.5f);
                        if (currentDeleteCharacter.equals(" ")) {
                            tts.speak(" Delete space ", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        else {
                            tts.speak(" Delete " + currentDeleteCharacter.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                        Log.d("isDeletionModeActive ", "deleteModeIndex" + deleteModeIndex);
                    }
                }
                else {
                    inScrollMode = false;
                    Log.d("no index finger scroll", "data" + data.dy.getInt());
                }

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
