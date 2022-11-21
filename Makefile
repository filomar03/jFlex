PACKAGE := com/filomar/interpreter
SRC_DIR := src
OUT_DIR := out/production/jFlex

SRCS := $(wildcard $(SRC_DIR)/$(PACKAGE)/*.java)
CLS := $(SRCS:$(SRC_DIR)/%.java=$(OUT_DIR)/%.class)

JC := javac
JCFLAGS := -d $(OUT_DIR)/ -cp $(SRC_DIR)/

.SUFFIXES: .java
.PHONY: program-run prompt-run generate-ast clean

program-run: $(CLS)
		java -cp $(OUT_DIR) com.filomar.interpreter.Flex scripts/main.flx

prompt-run: $(CLS)
		java -cp $(OUT_DIR) com.filomar.interpreter.Flex 


$(CLS): $(OUT_DIR)/%.class: $(SRC_DIR)/%.java
		@$(JC) $(JCFLAGS) $<

generate-ast: 
		@$(JC) $(JCFLAGS) src/com/filomar/tool/GenerateAst.java
		@java -cp $(OUT_DIR) com.filomar.tool.GenerateAst src/com/filomar/interpreter/ 

clean:
		@rm $(OUT_DIR)/$(PACKAGE)/*.class
		@rm $(OUT_DIR)/com/filomar/tool/GenerateAst.class