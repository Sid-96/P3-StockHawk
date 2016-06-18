package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.eazegraph.lib.charts.ValueLineChart;
import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class DetailStocksActivity extends AppCompatActivity {


    private ValueLineChart lineChart;
    private TextView errorMessage;
    private View progressCircle;
    private String companySymbol;
    private String companyName;
    private ArrayList<String> labels;
    private ArrayList<Float> values;
    private boolean isLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_stocks);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        lineChart = (ValueLineChart) findViewById(R.id.line_chart);
        errorMessage = (TextView) findViewById(R.id.error_message);
        progressCircle = findViewById(R.id.progress_circle);
        companySymbol = getIntent().getStringExtra("symbol");
        if(savedInstanceState==null)
            downloadStockDetails();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(isLoaded){
            outState.putString("company_name",companyName);
            outState.putStringArrayList("labels",labels);
            float[] valuesArray = new float[values.size()];
            for(int i=0 ; i<values.size() ; i++)
                valuesArray[i] = values.get(i);
            outState.putFloatArray("values_array",valuesArray);
        }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if(savedInstanceState!=null && savedInstanceState.containsKey("company_name")){
            isLoaded = true;
            companyName = savedInstanceState.getString("company_name");
            labels = savedInstanceState.getStringArrayList("labels");
            float[] valuesArray = savedInstanceState.getFloatArray("values_array");
            values = new ArrayList<>();
            for(int i=0 ; i<valuesArray.length ; i++)
                values.add(valuesArray[i]);
            onDownloadCompleted();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void downloadStockDetails() {
        StringBuilder urlStringBuilder = new StringBuilder();
        urlStringBuilder.append("http://chartapi.finance.yahoo.com/instrument/1.0/");
        urlStringBuilder.append(companySymbol);
        urlStringBuilder.append("/chartdata;type=quote;range=5y/json");

        OkHttpClient client = new OkHttpClient();
        client.newCall(new Request.Builder()
        .url(urlStringBuilder.toString())
        .build()).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                onDownloadFailed();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        // Trim response string
                        String result = response.body().string();
                        if (result.startsWith("finance_charts_json_callback( ")) {
                            result = result.substring(29, result.length() - 2);
                        }

                        // Parse JSON
                        JSONObject object = new JSONObject(result);
                        companyName = object.getJSONObject("meta").getString("Company-Name");
                        labels = new ArrayList<>();
                        values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject seriesItem = series.getJSONObject(i);
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyyMMdd");
                            String date = android.text.format.DateFormat.
                                    getMediumDateFormat(getApplicationContext()).
                                    format(srcFormat.parse(seriesItem.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(seriesItem.getString("close")));
                        }

                        onDownloadCompleted();
                    } catch (Exception e) {
                        onDownloadFailed();
                        e.printStackTrace();
                    }
                } else {
                    onDownloadFailed();
                }
            }
        });
    }

    private void onDownloadCompleted() {
        DetailStocksActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setTitle(companyName);
                progressCircle.setVisibility(View.GONE);
                errorMessage.setVisibility(View.GONE);

                ValueLineSeries series = new ValueLineSeries();
                series.setColor(0xFF56B7F1);
                for (int i = 0; i < labels.size(); i++) {
                    series.addPoint(new ValueLinePoint(labels.get(i), values.get(i)));
                }
                lineChart.addSeries(series);
                lineChart.setVisibility(View.VISIBLE);
                lineChart.startAnimation();
                isLoaded = true;

            }
        });
    }

    private void onDownloadFailed() {
        DetailStocksActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineChart.setVisibility(View.GONE);
                progressCircle.setVisibility(View.GONE);
                errorMessage.setVisibility(View.VISIBLE);
                setTitle(R.string.error);
            }
        });
    }
}
