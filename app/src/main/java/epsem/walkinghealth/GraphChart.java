package epsem.walkinghealth;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


public class GraphChart {
    private XYSeries xSeries = new XYSeries("X");
    private XYSeries ySeries = new XYSeries("Y");
    private XYSeries zSeries = new XYSeries("Z");
    private View graph;

    public GraphChart(Context context) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(xSeries);
        dataset.addSeries(ySeries);
        dataset.addSeries(zSeries);
        XYSeriesRenderer xRenderer = new XYSeriesRenderer();
        XYSeriesRenderer yRenderer = new XYSeriesRenderer();
        XYSeriesRenderer zRenderer = new XYSeriesRenderer();
        yRenderer.setColor(Color.RED);
        zRenderer.setColor(Color.GREEN);
        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        multiRenderer.addSeriesRenderer(xRenderer);
        multiRenderer.addSeriesRenderer(yRenderer);
        multiRenderer.addSeriesRenderer(zRenderer);
        graph = ChartFactory.getLineChartView(context, dataset, multiRenderer);

    }
    public void clear(){
        xSeries.clear();
        ySeries.clear();
        zSeries.clear();
    }

    public View getView(){
        return this.graph;
    }

    public void add(long t, double x,double y, double z){
        xSeries.add(t, x);
        ySeries.add(t, y);
        zSeries.add(t, z);
    }

    public void update() {
        ((GraphicalView) graph).repaint();
    }
}