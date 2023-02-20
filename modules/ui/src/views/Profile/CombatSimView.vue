<template>
  <h2 class="text-xl my-4 px-1">Combat simulation</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="dataEnabled"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Data simulation</span>
      <select
        class="select w-1/4 max-w-xs"
        v-model="dataType"
        :disabled="!dataEnabled"
      >
        <option v-for="(mode, index) in combatSimData" :key="index">
          {{ mode }}
        </option>
      </select>
    </label>
  </div>
  <div class="divider-horizontal" />
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Neural fragment</span>
      <select
        class="select w-1/4 max-w-xs"
        v-model="neuralType"
        :disabled="!dataEnabled"
      >
        <option v-for="(mode, index) in combatSimNeural" :key="index">
          {{ mode }}
        </option>
      </select>
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Echelon to use</span>
      <select
        class="select w-1/4 max-w-xs"
        v-model="neuralEchelon"
        :disabled="!dataEnabled"
      >
        <option
          v-for="(echelon, index) in [
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
          ]"
          :key="index"
        >
          {{ echelon }}
        </option>
      </select>
    </label>
  </div>
  <h2 class="text-xl my-4 px-1">Coalition drill</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="coalitionEnabled"
        :true-value="true"
        :false-value="false"
        :disabled="!dataEnabled"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Type</span>
      <select
        class="select w-1/4 max-w-xs"
        v-model="coalitionPreferredType"
        :disabled="!dataEnabled"
      >
        <option v-for="(mode, index) in combatSimCoalition" :key="index">
          {{ mode }}
        </option>
      </select>
    </label>
  </div>
</template>
<script setup lang="ts">
import { useClassifierStore } from "@/stores/classifiers";
import { useProfileStore } from "@/stores/profile";
import { computed } from "vue";

const { combatSimData, combatSimNeural, combatSimCoalition } =
  useClassifierStore();

const store = useProfileStore();

const dataEnabled = computed({
  get() {
    return store.combatSimDataEnabled;
  },
  set(value) {
    store.setCombatSimDataEnabled(value);
  },
});

const dataType = computed({
  get() {
    return store.combatSimDataType;
  },
  set(value) {
    store.setCombatSimDataType(value);
  },
});

const neuralType = computed({
  get() {
    return store.combatSimNeuralType;
  },
  set(value) {
    store.setCombatSimNeuralType(value);
  },
});

const neuralEchelon = computed({
  get() {
    return store.combatSimNeuralEchelon;
  },
  set(value) {
    store.setCombatSimNeuralEchelon(value);
  },
});

const coalitionEnabled = computed({
  get() {
    return store.combatSimCoalitionEnabled;
  },
  set(value) {
    store.setCombatSimCoalitionEnabled(value);
  },
});

const coalitionPreferredType = computed({
  get() {
    return store.combatSimCoalitionPreferredTYpe;
  },
  set(value) {
    store.setCombatSimCoalitionPreferredType(value);
  },
});
</script>
