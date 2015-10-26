package epsem.walkinghealth;

import android.content.Context;
import android.view.View;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

/**
 * Created by vicenc on 15/10/15.
 */
public class GraphChart {
    private XYSeries xSeries = new XYSeries("X");
    private View graph;

    public GraphChart(Context context) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(xSeries);
        XYSeriesRenderer xRenderer = new XYSeriesRenderer();
        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        multiRenderer.addSeriesRenderer(xRenderer);
        graph = ChartFactory.getLineChartView(context, dataset, multiRenderer);

    }
    public void clear(){
        xSeries.clear();
    }

    public View getView(){
        return this.graph;
    }

    public void add(long t, double x){
        xSeries.add(t, x);
    }

    public void update() {
        ((GraphicalView) graph).repaint();
    }
}