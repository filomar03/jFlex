PACKAGE := com/filomar/interpreter
SRC_DIR := src
OUT_DIR := out/production/jFlex

SRCS := $(wildcard $(SRC_DIR)/$(PACKAGE)/*.java)
CLS := $(SRCS:$(SRC_DIR)/%.java=$(OUT_DIR)/%.class)

JC := javac
JCFLAGS := -d $(OUT_DIR)/ -cp $(SRC_DIR)/

EXPORT_DIR := ~/Desktop/jflex

.SUFFIXES: .java
.PHONY: export-interpreter generate-ast clear

export-interpreter: $(CLS)
		@mkdir -p $(EXPORT_DIR)
		@rm -r -f $(EXPORT_DIR)/
		@cp -r $(OUT_DIR)/ $(EXPORT_DIR)
		@rm -r -f $(OUT_DIR)/com/filomar/tool
		@cp jflex $(EXPORT_DIR)

$(CLS): $(OUT_DIR)/%.class: $(SRC_DIR)/%.java
		@$(JC) $(JCFLAGS) $<

generate-ast:
		@$(JC) $(JCFLAGS) src/com/filomar/tool/GenerateAst.java
		@java -cp $(OUT_DIR) com.filomar.tool.GenerateAst src/com/filomar/interpreter/

clear:
		@rm $(OUT_DIR)/$(PACKAGE)/*.class
		@rm $(OUT_DIR)/com/filomar/tool/GenerateAst.class

