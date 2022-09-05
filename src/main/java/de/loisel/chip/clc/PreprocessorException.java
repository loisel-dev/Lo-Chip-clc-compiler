package de.loisel.chip.clc;

public class PreprocessorException extends RuntimeException {
    /**
     * Constructs a new Exception when there was an error
     * during preprocessing
     */
    public PreprocessorException(Line line, String message) {
        super("File " + line.fName + ": Preprocessor in line " + line + ": " + message);
    }
}
