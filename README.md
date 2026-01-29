
This repository contains all code, analysis, and data related to our paper submitted to **ISSTA 2026**.

## Repository Structure

### `RawCodes/`
This directory contains:

- **`Location/`**  
  Code based on **FlowDroid**, implementing enhancements to taint analysis for detecting **location leaks** using **off-the-call-path context**.  

- **`PTA_QilinEnhancement/`**  
  Java classes added to the **Qilin** framework to support **off-the-call-path context sensitivity** in pointer analysis.

### `JarFiles/`
This directory contains all the JAR files used in our experiments. These JARs were generated from the code implemented in the `RawCodes/` directory.

### `RawData/`
This directory includes all **raw data** from the experiments and analyses presented in the paper.
