package luka.teum.dl_service.ai.util;

import messaging.Solution;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DataSetUtil {

    public static final int BATCH_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(DataSetUtil.class);

    public static DataSetIterator getDataSetIterator(final String directoryPath, int width, int height) throws IOException {
        File directory = new File(directoryPath);
        File[] digitsDirectory = directory.listFiles();

        if (digitsDirectory == null || digitsDirectory.length == 0) {
            return createEmptyIterator();
        }

        List<DataSet> dataSets = new ArrayList<>();
        for (File digitDirectory : digitsDirectory) {
            DataSet dataSet = getDataSet(digitDirectory.getPath(), width, height);
            List<DataSet> adding = dataSet.asList();
            dataSets.addAll(adding);
            log.info("Add {} data from: {}", adding.size(), digitDirectory.getName());
        }

        return createShuffledIterator(dataSets);
    }

    public static DataSetIterator getDataSetIterator(DataSet dataSet) {
        final List<DataSet> dataSets = dataSet.asList();
        Collections.shuffle(dataSets, new Random(System.currentTimeMillis()));

        return new ListDataSetIterator<>(dataSets, BATCH_SIZE);
    }

    private static DataSetIterator createEmptyIterator() {
        return new ListDataSetIterator<>(Collections.emptyList(), BATCH_SIZE);
    }

    private static DataSetIterator createShuffledIterator(List<DataSet> dataSets) {

        Collections.shuffle(dataSets, new Random(System.currentTimeMillis()));

        return new ListDataSetIterator<>(dataSets, BATCH_SIZE);
    }

    public static DataSet getDataSet(final String directoryPath, int width, int height) throws IOException {
        File directory = new File(directoryPath);
        File[] images = directory.listFiles();

        if (images == null) {
            return new DataSet();
        }

        int nSamples = images.length;
        int digit = Integer.parseInt(directory.getName());
        NativeImageLoader nativeImageLoader = new NativeImageLoader(height, width);
        ImagePreProcessingScaler imagePreProcessingScaler = new ImagePreProcessingScaler(0, 1);
        INDArray inputData = Nd4j.create(nSamples, height * width);
        INDArray outputData = Nd4j.create(nSamples, Solution.SUDOKU_SIZE + 1);

        int n = 0;
        for (File image : images) {
            INDArray img = nativeImageLoader.asRowVector(image);
            imagePreProcessingScaler.transform(img);
            inputData.putRow(n, img);
            outputData.put(n, digit, 1.0);
            n++;
        }
        return new DataSet(inputData, outputData);
    }



}
