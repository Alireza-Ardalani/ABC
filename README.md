
This repository contains all code, analysis, and data related to our paper submitted to **ICSE 2026**.

## Repository Structure

### `RawCodes/`
This directory contains:

- **`Location_SafeBlock/`**  
  Code based on **FlowDroid**, implementing enhancements to taint analysis for detecting **location leaks** using **off-the-call-path context**.  
  It also includes support for detecting **safe vs. unsafe blocking methods**.

- **`PTA_QilinEnhancement/`**  
  Java classes added to the **Qilin** framework to support **off-the-call-path context sensitivity** in pointer analysis.

### `JarFiles/`
This directory contains all the JAR files used in our experiments. These JARs were generated from the code implemented in the `RawCodes/` directory.

### `RawData/`
This directory includes all **raw data** from the experiments and analyses presented in the paper.