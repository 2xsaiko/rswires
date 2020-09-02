LAYERGEN = layergen

COLORMAPS = colormaps
INCLUDE = include
INSTALL_PATH = ../../src/main/resources/assets/rswires/textures/block

t_base = base
t_left = left_off left_on
t_back = back_off back_on
t_right = right_off right_on
t_output = output_off output_on
t_alt1 = alt1_off alt1_on

templates_all = $(t_base) $(t_left) $(t_back) $(t_right) $(t_output)

.DEFAULT_GOAL := all

define target_template
$(1)_$(2).png: $(1).in.png $(INCLUDE) $(COLORMAPS)/$(2).yml
	$(LAYERGEN) $(1).in.png -I$(INCLUDE) -m$(COLORMAPS)/$(2).yml -o $(1)_$(2).png

install:: $(INSTALL_PATH)/$(1)
	cp $(1)_$(2).png $(INSTALL_PATH)/$(1)/$(2).png

targets += $(1)_$(2).png
endef

define gate_template
$(foreach template,$(strip $(2)),$(eval $(call target_template,$(strip $(1)),$(template))))
endef

gates = and_gate or_gate nand_gate nor_gate xor_gate xnor_gate

$(call gate_template, and_gate, $(templates_all))
$(call gate_template, or_gate, $(templates_all))
$(call gate_template, nand_gate, $(templates_all))
$(call gate_template, nor_gate, $(templates_all))
$(call gate_template, xor_gate, $(t_base) $(t_left) $(t_right) $(t_alt1) $(t_output))
$(call gate_template, xnor_gate, $(t_base) $(t_left) $(t_right) $(t_alt1) $(t_output))

all: $(targets)

clean:
	$(RM) $(targets)

$(INSTALL_PATH):
	mkdir -p $@

$(foreach gate,$(gates),$(INSTALL_PATH)/$(gate)): $(INSTALL_PATH)
	mkdir -p $@

.PHONY: install
