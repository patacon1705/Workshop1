package com.udistrital.workshop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BioinformaticsWorkshop {

    private static double probA;
    private static double probT;
    private static double probC;
    private static double probG;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Ingresar probabilidades
        inputProbabilities(scanner);

        // Rango de generación de secuencias
        System.out.println("Ingrese el tamaño de secuencia (entre 1000 y 2000000): ");
        int seqLength = scanner.nextInt();

        System.out.println("Ingrese el motif a buscar (por ejemplo, 'ACGT'): ");
        String motif = scanner.next();

        int numSequences = 1000; // Puedes ajustar el número de secuencias si lo deseas

        // Generación concurrente de secuencias
        ArrayList<String> sequences = generateSequencesConcurrently(numSequences, seqLength);

        // Guardar secuencias en un archivo .txt
        saveToTxt(sequences);

        // Buscar motif en las secuencias generadas
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ArrayList<Future<Result>> results = new ArrayList<>();

        for (String sequence : sequences) {
            Future<Result> future = executor.submit(() -> {
                int motifCount = findMotif(sequence, motif);
                double entropy = calculateEntropy(sequence);
                return new Result(sequence, motifCount, entropy);
            });
            results.add(future);
        }

        // Esperar y procesar los resultados
        String bestSequence = "";
        int maxCount = 0;
        double bestEntropy = 0;

        for (Future<Result> future : results) {
            Result result = future.get();
            if (result.motifCount > maxCount) {
                maxCount = result.motifCount;
                bestSequence = result.sequence;
                bestEntropy = result.entropy;
            }
        }

        // Mostrar los resultados
        System.out.println("Secuencia con más motifs: " + bestSequence);
        System.out.println("Motif aparece: " + maxCount + " veces");
        System.out.println("Entropía de la secuencia: " + bestEntropy);

        executor.shutdown();
    }

    public static void inputProbabilities(Scanner scanner) {
        double sum;
        do {
            System.out.println("Ingrese la probabilidad para la base A (entre 0 y 1, con hasta 3 decimales): ");
            probA = getValidProbability(scanner);

            System.out.println("Ingrese la probabilidad para la base T (entre 0 y 1, con hasta 3 decimales): ");
            probT = getValidProbability(scanner);

            System.out.println("Ingrese la probabilidad para la base C (entre 0 y 1, con hasta 3 decimales): ");
            probC = getValidProbability(scanner);

            System.out.println("Ingrese la probabilidad para la base G (entre 0 y 1, con hasta 3 decimales): ");
            probG = getValidProbability(scanner);

            sum = Math.round((probA + probT + probC + probG) * 1000.0) / 1000.0; // Redondea a tres decimales
            if (sum != 1) {
                System.out.println("La suma de las probabilidades debe ser exactamente 1. Vuelva a intentarlo.");
            }
        } while (sum != 1);
    }

    public static double getValidProbability(Scanner scanner) {
        double probability;
        do {
            probability = scanner.nextDouble();
            if (probability < 0 || probability > 1) {
                System.out.println("La probabilidad debe estar entre 0 y 1. Inténtelo de nuevo.");
            }
        } while (probability < 0 || probability > 1);
        return Math.round(probability * 1000.0) / 1000.0; // Redondea a tres decimales
    }

    public static ArrayList<String> generateSequencesConcurrently(int n, int m) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ArrayList<Future<String>> futures = new ArrayList<>();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < n; i++) {
            Future<String> future = executor.submit(() -> {
                StringBuilder sequence = new StringBuilder();
                for (int j = 0; j < m; j++) {
                    double r = secureRandom.nextDouble();
                    if (r < probA) {
                        sequence.append('A');
                    } else if (r < probA + probT) {
                        sequence.append('T');
                    } else if (r < probA + probT + probC) {
                        sequence.append('C');
                    } else {
                        sequence.append('G');
                    }
                }
                return sequence.toString();
            });
            futures.add(future);
        }

        ArrayList<String> sequences = new ArrayList<>();
        for (Future<String> future : futures) {
            sequences.add(future.get());
        }

        executor.shutdown();
        return sequences;
    }

    public static int findMotif(String sequence, String motif) {
        int count = 0;
        int index = 0;
        while ((index = sequence.indexOf(motif, index)) != -1) {
            count++;
            index += motif.length();
        }
        return count;
    }

    public static double calculateEntropy(String sequence) {
        int[] counts = new int[4]; // A, C, G, T
        for (char c : sequence.toCharArray()) {
            switch (c) {
                case 'A': counts[0]++; break;
                case 'C': counts[1]++; break;
                case 'G': counts[2]++; break;
                case 'T': counts[3]++; break;
            }
        }
        double length = sequence.length();
        double entropy = 0;
        for (int count : counts) {
            double freq = count / length;
            if (freq > 0) {
                entropy -= freq * (Math.log(freq) / Math.log(2));
            }
        }
        return entropy;
    }

    public static void saveToTxt(ArrayList<String> sequences) {
        File file = new File("database.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (String sequence : sequences) {
                bw.write(sequence + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class Result {
        String sequence;
        int motifCount;
        double entropy;

        public Result(String sequence, int motifCount, double entropy) {
            this.sequence = sequence;
            this.motifCount = motifCount;
            this.entropy = entropy;
        }
    }
}



