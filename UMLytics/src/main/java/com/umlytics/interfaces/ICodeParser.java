package com.umlytics.interfaces;

import com.umlytics.domain.valueobjects.UMLModel;
import java.io.File;
import java.util.List;

public interface ICodeParser {
    UMLModel parse(List<File> files);
    List<String> getSupportedLanguages();
}
