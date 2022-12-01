PACKAGE := com/filomar/interpreter
SRC_DIR := src
OUT_DIR := out/production/jFlex

SRCS := $(wildcard $(SRC_DIR)/$(PACKAGE)/*.java)
CLS := $(SRCS:$(SRC_DIR)/%.java=$(OUT_DIR)/%.class)

JC := javac
JCFLAGS := -d $(OUT_DIR)/ -cp $(SRC_DIR)/

.SUFFIXES: .java
.PHONY: interpreter-generate entry-generate generate-ast clean

interpreter-generate: $(CLS)


$(CLS): $(OUT_DIR)/%.class: $(SRC_DIR)/%.java
		@$(JC) $(JCFLAGS) $<

entry-generate:
		echo java -cp $(OUT_DIR) com.filomar.interpreter.Flex >> jflex

ast-generate:
		@$(JC) $(JCFLAGS) src/com/filomar/tool/GenerateAst.java
		@java -cp $(OUT_DIR) com.filomar.tool.GenerateAst src/com/filomar/interpreter/ 

clear:
		@rm $(OUT_DIR)/$(PACKAGE)/*.class
		@rm $(OUT_DIR)/com/filomar/tool/GenerateAst.class
