package com.eliedersousa.painel;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GraphFragment extends Fragment implements FileListAdapter.OnItemClickListener {
    private final List<String> fileList = new ArrayList<>();

    public LineChart mChart;
    public int dataCounter = 0;
    public ArrayList<Entry> wattArray = new ArrayList<>();
    public ArrayList<Entry> velArray = new ArrayList<>();
    public ArrayList<Entry> ampArray = new ArrayList<>();
    public ArrayList<Entry> voltArray = new ArrayList<>();
    public ArrayList<Entry> tempArray = new ArrayList<>();

    private ItemTouchHelper mItemTouchHelper;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public GraphFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GraphFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GraphFragment newInstance(String param1, String param2) {
        GraphFragment fragment = new GraphFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_filelist);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Get file names and add them to the list
        File root = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        assert root != null;
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(removeExtension(file.getName()));
            }
        }

        // Create and set the adapter
        FileListAdapter adapter = new FileListAdapter(getActivity(), fileList, this);
        recyclerView.setAdapter(adapter);

        // Set up ItemTouchHelper with the adapter and callback
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mChart = (LineChart) view.findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);
        mChart.getDescription().setEnabled(false);

        return view;
    }

    public void onItemClick(View view, int position, String fileName) {
        fileName += ".txt";
        File root = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File clickedFile = new File(root, fileName);

        try {
            FileInputStream fileInputStream = new FileInputStream(clickedFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String line;

            dataCounter = 0;

            velArray.clear();
            ampArray.clear();
            voltArray.clear();
            wattArray.clear();
            tempArray.clear();

            while ((line = bufferedReader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length == 6) {
                    velArray.add(new Entry(dataCounter, Integer.parseInt(values[1])));
                    ampArray.add(new Entry(dataCounter, Float.parseFloat(values[2])));
                    voltArray.add(new Entry(dataCounter, Float.parseFloat(values[3])));
                    wattArray.add(new Entry(dataCounter, Float.parseFloat(values[4])));
                    tempArray.add(new Entry(dataCounter, Float.parseFloat(values[5])));
                }
                dataCounter++;
            }

            // Close the readers
            bufferedReader.close();
            inputStreamReader.close();
            fileInputStream.close();

            renderData();
        } catch (IOException | NumberFormatException e) {
            showToast("Error reading file: " + e.getMessage());
        }
    }

    private String removeExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return fileName.substring(0, lastDotIndex);
        } else {
            return fileName; // If there is no dot in the file name, return the original name
        }
    }

    public void renderData() {
        mChart.invalidate();
        mChart.clear();

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

    public void showToast( String msg ) {
        Toast.makeText( getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}