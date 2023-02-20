<template>
  <h2 class="text-xl my-4 px-1">Factory</h2>

  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable T-Doll enhancement</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="dollEnhancement"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable T-Doll disassembly</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="dollDisassembly"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable 4 Star T-DOll disassembly</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="doll4StarDisassembly"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Always disassemble after enhance</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="dollDisassembleAfterEnhance"
        :true-value="true"
        :false-value="false"
        :disabled="!dollEnhancement"
      />
    </label>
  </div>
  <h2 class="text-xl my-4 px-1">Equipment</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable Equipment disassembly</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="equipmentDisassembly"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable 4 Start Equipment disassembly</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="equipment4StarDisassembly"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
</template>
<script setup lang="ts">
import { useProfileStore } from "@/stores/profile";
import { computed } from "vue";

const store = useProfileStore();

const dollEnhancement = computed({
  get() {
    return store.factoryEnhancementEnabled;
  },
  set(value) {
    store.setFactoryEnhancementEnabled(value);
  },
});
const dollDisassembly = computed({
  get() {
    return store.factoryDisassemblyEnabled;
  },
  set(value) {
    store.setFactoryDisassemblyEnabled(value);
  },
});
const doll4StarDisassembly = computed({
  get() {
    return store.factoryDisassembly4StarEnabled;
  },
  set(value) {
    store.setFactoryDisassembly4StarEnabled(value);
  },
});
const dollDisassembleAfterEnhance = computed({
  get() {
    return store.factoryAlwaysDisassembleAfterEnhance;
  },
  set(value) {
    store.setFactoryAlwaysDisassembleAfterEnhance(value);
  },
});
const equipmentDisassembly = computed({
  get() {
    return store.factoryEquipmentDisassemblyEnabled;
  },
  set(value) {
    store.setFactoryEquipmentDisassemblyEnabled(value);
  },
});
const equipment4StarDisassembly = computed({
  get() {
    return store.factoryEquipmentDisassembly4StarEnabled;
  },
  set(value) {
    store.setFactoryEquipmentDisassembly4StarEnabled(value);
  },
});
</script>
