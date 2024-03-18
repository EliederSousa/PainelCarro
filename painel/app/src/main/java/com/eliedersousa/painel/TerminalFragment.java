package com.eliedersousa.painel;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import android.content.pm.PackageManager;

public class TerminalFragment extends Fragment implements SerialInputOutputManager.Listener {
    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = "com.eliedersousa.painel.GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;
    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;

    private TextView speedText, ampText, voltText, wattText, tempText;
    private Button viewButton, newButton;

    String stringAccumulator = "";
    String currentDateAndTime = "";

    Double amp;
    Double volt;
    Double watt;
    Double temp;
    int vel;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    public LineChart mChart;
    public int dataCounter = 0;
    public ArrayList<Entry> wattArray = new ArrayList<>();
    public ArrayList<Entry> velArray = new ArrayList<>();

    UsbDevice device;
    int port = 0;
    UsbSerialDriver driver;


    public TerminalFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        try {
            UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
            UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
            UsbSerialProber usbCustomProber = CustomProber.getCustomProber();
            for(UsbDevice deviceTemp : usbManager.getDeviceList().values()) {
                UsbSerialDriver driverTemp = usbDefaultProber.probeDevice(deviceTemp);
                if(driverTemp == null) {
                    driverTemp = usbCustomProber.probeDevice(deviceTemp);
                }
                driver = driverTemp;
                device = deviceTemp;
                port = 0;
            }
            if(driver == null) {
                showToast("Driver não encontrado.");
            }

            deviceId = device.getDeviceId();
            portNum = port;
            baudRate = 19200;
            withIoManager = new Boolean(false);
        } catch (Exception e) {
            //showToast("Erro onCreate" + e.getMessage());
        }
    }

    public void renderData() {
        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(5f, 5f, 0f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setAxisMaximum(200f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.enableGridDashedLine(5f, 5f, 0f);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawLimitLinesBehindData(false);

        mChart.getAxisRight().setEnabled(false);
        setData();
    }

    private void setData() {
        LineDataSet wattLineDataSet;
        LineDataSet velLineDataSet;
        if (mChart.getData() != null && mChart.getData().getDataSetCount() > 0) {
            wattLineDataSet = (LineDataSet) mChart.getData().getDataSetByIndex(0);
            velLineDataSet = (LineDataSet) mChart.getData().getDataSetByIndex(1);
            wattLineDataSet.setValues(wattArray);
            velLineDataSet.setValues(velArray);

            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
            if( wattArray.size() > 20 ) {
                float wattMean = 0.0f;
                for( int w=0; w<20; w++ ) wattMean += wattArray.get(wattArray.size()-1-w).getY();
                wattMean /= 20;
                mChart.moveViewTo(wattArray.size() - 1, wattMean, mChart.getAxisLeft().getAxisDependency());
            }
        } else {
            wattLineDataSet = new LineDataSet(wattArray, "Potência (Watts)");
            wattLineDataSet.setDrawIcons(false);
            wattLineDataSet.setDrawValues(false);
            //wattLineDataSet.enableDashedLine(10f, 5f, 0f);
            wattLineDataSet.enableDashedHighlightLine(10f, 5f, 0f);
            wattLineDataSet.setColor(Color.RED);
            wattLineDataSet.setCircleColor(Color.RED);
            wattLineDataSet.setLineWidth(1f);
            wattLineDataSet.setCircleRadius(1f);
            wattLineDataSet.setDrawCircleHole(false);
            //wattLineDataSet.setValueTextSize(9f);
            wattLineDataSet.setDrawFilled(false);
            wattLineDataSet.setFormLineWidth(1f);
            wattLineDataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            wattLineDataSet.setFormSize(15.f);
            //wattLineDataSet.setFillColor(Color.DKGRAY);

            velLineDataSet = new LineDataSet(velArray, "Velocidade");
            velLineDataSet.setDrawIcons(false);
            velLineDataSet.setDrawValues(false);
            velLineDataSet.enableDashedHighlightLine(10f, 5f, 0f);
            velLineDataSet.setColor(Color.BLUE);
            velLineDataSet.setCircleColor(Color.BLUE);
            velLineDataSet.setLineWidth(1f);
            velLineDataSet.setCircleRadius(1f);
            velLineDataSet.setDrawCircleHole(false);
            //wattLineDataSet.setValueTextSize(9f);
            velLineDataSet.setDrawFilled(false);
            velLineDataSet.setFormLineWidth(1f);
            velLineDataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            velLineDataSet.setFormSize(15.f);

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(wattLineDataSet);
            dataSets.add(velLineDataSet);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)
            mainLooper.post(this::connect);
        renderData();
    }

    @Override
    public void onPause() {
        if(connected) {
            showToast("Desconectado.");
            disconnect();
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        speedText = (TextView) view.findViewById(R.id.text_speed);
        ampText = (TextView) view.findViewById(R.id.text_amp);
        voltText = (TextView) view.findViewById(R.id.text_volt);
        wattText = (TextView) view.findViewById(R.id.text_watt);
        tempText = (TextView) view.findViewById(R.id.text_temp);

        viewButton = (Button) view.findViewById(R.id.btnView);
        newButton  = (Button) view.findViewById(R.id.btnNew);

        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new GraphFragment();
                getParentFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "graph").addToBackStack(null).commit();
            }
        });

        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newFile();
            }
        });


        mChart = (LineChart) view.findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);
        mChart.getDescription().setEnabled(false);
        return view;
    }

    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> receive(data));
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(() -> {
            //showToast("Erro: " + e.getMessage());
            disconnect();
        });
    }

    private void connect() {
        UsbManager usbManager = (UsbManager) requireActivity().getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
        UsbSerialProber usbCustomProber = CustomProber.getCustomProber();
        UsbSerialDriver driver = null;
        for(UsbDevice device : usbManager.getDeviceList().values()) {
            driver = usbDefaultProber.probeDevice(device);
            if(driver == null) {
                driver = usbCustomProber.probeDevice(device);
            }
            deviceId = device.getDeviceId();
            portNum = 0;
            baudRate = 19200;
            withIoManager = true;
        }

        if(driver == null) {
            //showToast("Conexão falhou: driver não encontrado.");
            return;
        }

        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(INTENT_ACTION_GRANT_USB), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                showToast("Conexão falhou: permissão negada.");
            else
                //showToast("Falha na abertura.");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            try{
                usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            }catch (UnsupportedOperationException e){
                //showToast("Parâmetros não suportados de conexão.");
            }
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            showToast("Conectado.");
            connected = true;
        } catch (Exception e) {
            //showToast("Falha na conexão: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    private void send(String str) {
        try {
            if(!connected) {
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] data = (str + '\n').getBytes();
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    public String jsonString = "";
    public int stateJson = 0;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void receive(byte[] data) {
        if (data.length > 0) {
            String s = new String(data);
            // stateJson controla em qual momento estamos lendo os dados
            if ( stateJson == 0 ) {
                if(s.contains("<")) {
                    stateJson = 1;
                    if(s.contains(">")) {
                        stateJson = 2;
                        // String está completa
                        jsonString = s.substring(1, s.indexOf(">"));
                    } else {
                        jsonString = s.substring(1);
                    }
                }
            } else if ( stateJson == 1 ) {
                // String já tinha começado
                if( s.contains(">") ) {
                    stateJson = 2;
                    jsonString += s.substring(0, s.indexOf(">"));
                } else {
                    jsonString += s;
                }
            }
            if (stateJson == 2) {
                try {
                    JSONObject js = new JSONObject(jsonString);
                    jsonString = "";
                    stateJson = 0;

                    vel = js.has("vel") ? (int) Math.round(Double.parseDouble(js.getString("vel"))) : 0;
                    amp = js.has("amp") ? Double.parseDouble( js.getString("amp") ) : 0;
                    volt = js.has("volt") ? Double.parseDouble( js.getString("volt") ) : 0;
                    temp = js.has("temp") ? Double.parseDouble( js.getString("temp") ) : 0;
                    watt = amp * volt;

                    wattArray.add(new Entry(dataCounter, watt.floatValue() ));
                    velArray.add(new Entry(dataCounter++, vel ));

                    speedText.setText( String.format("%s", vel) );
                    ampText.setText( String.format("%.1fA", amp) );
                    voltText.setText( String.format("%.1fV", volt) );
                    tempText.setText( String.format("%.1fº", temp) );
                    wattText.setText( String.format("%.0fW", watt) );

                    stringAccumulator += dataCounter + "," + vel + "," + amp + "," + volt + "," + watt.intValue() + "," + temp + "\r\n";

                } catch (JSONException e) {
                    //showToast("Erro: " + Objects.requireNonNull(e.getMessage()));
                    stateJson = 0;
                    jsonString = "";
                }

                if (Objects.equals(currentDateAndTime, "")) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
                    currentDateAndTime = dateFormat.format(new Date());
                }

                if( dataCounter % 100 == 0 && (dataCounter > 0) ) {
                    WriteToFile(currentDateAndTime + ".txt", stringAccumulator);
                    stringAccumulator = "";
                }
            }
        }
        renderData();
    }

    public void newFile() {
        if(!Objects.equals(currentDateAndTime, ""))
            WriteToFile(currentDateAndTime + ".txt", stringAccumulator);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
        currentDateAndTime = dateFormat.format(new Date());
        stateJson = 0;
        jsonString = "";
        dataCounter = 0;
        stringAccumulator = "";
        wattArray.clear();
        velArray.clear();
    }

    public void WriteToFile(String fileName, String content) {
        File root = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        try {
            if (root != null) {
                File fileDir = new File(root.getAbsolutePath(), fileName);
                FileOutputStream file = new FileOutputStream(fileDir, true);
                file.write(content.getBytes());
                file.close();
            } else {
                showToast("External storage not available");
            }
        } catch (IOException e) {
            showToast("Error: " + e.getMessage());
        }
    }

    private void showToast( String msg ) {
        Toast.makeText( getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}
