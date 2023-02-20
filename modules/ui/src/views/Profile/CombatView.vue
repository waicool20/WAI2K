<template>
  <h2 class="text-xl my-4 px-1">Combat</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enabled</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="enabled"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Map</span>
      <select class="select w-1/4 max-w-xs" v-model="map">
        <optgroup label="Normal">
          <option v-for="(mapItem, key) in sortedMaps.normal" :key="key">
            {{ mapItem.name }}
          </option>
        </optgroup>
        <optgroup label="Emergency">
          <option v-for="(mapItem, key) in sortedMaps.emergency" :key="key">
            {{ mapItem.name }}
          </option>
        </optgroup>
        <optgroup label="Night">
          <option v-for="(mapItem, key) in sortedMaps.night" :key="key">
            {{ mapItem.name }}
          </option>
        </optgroup>
        <optgroup label="Campaign">
          <option v-for="(mapItem, key) in sortedMaps.campaign" :key="key">
            {{ mapItem.name }}
          </option>
        </optgroup>
        <optgroup label="Event">
          <option v-for="(mapItem, key) in sortedMaps.event" :key="key">
            {{ mapItem.name }}
          </option>
        </optgroup>
      </select>
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Repair threshold (%)</span>
      <input
        v-model="repairThreshold"
        type="number"
        step="1"
        min="1"
        max="100"
        class="input w-1/4 max-w-xs"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Battle timeout (s)</span>
      <input
        v-model="battleTimeout"
        type="number"
        step="1"
        min="1"
        max="500"
        class="input w-1/4 max-w-xs"
      />
    </label>
  </div>
  <h2 class="text-xl my-4 px-1">Draggers</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Dragger slot</span>
      <span class="btn-group">
        <input
          v-for="slot in draggerSlots"
          type="radio"
          name="draggerSlot"
          :data-title="slot"
          class="btn bg-neutral-focus w-12"
          v-model="draggerSlot"
          :key="slot"
          :value="slot"
        />
      </span>
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Name</span>
      <select class="select w-1/4 max-w-xs" v-model="firstDragger">
        <option
          v-for="(doll, index) in dolls"
          :key="index"
          :value="{ id: doll.id }"
        >
          {{ doll.id }}
        </option>
      </select>
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text" />
      <button class="btn btn-primary btn-xs rounded-full w-8 h-8" @click="() => store.swapCombatDraggers()">
        <font-awesome-icon icon="fa-solid fa-arrows-up-down" />
      </button>
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Name</span>
      <select class="select w-1/4 max-w-xs" v-model="secondDragger">
        <option
          v-for="(doll, index) in dolls"
          :key="index"
          :value="{ id: doll.id }"
        >
          {{ doll.id }}
        </option>
      </select>
    </label>
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import natsort from "natsort";
import { useProfileStore } from "@/stores/profile";
import { useClassifierStore } from "@/stores/classifiers";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
const store = useProfileStore();
const { dolls, maps } = useClassifierStore();
const sorter = natsort();
const sortedMaps = computed({
  get() {
    const normal = ref(maps.normal);
    const emergency = ref(maps.emergency);
    const night = ref(maps.night);
    const campaign = ref(maps.campaign);
    const event = ref(maps.event);
    return {
      normal: normal.value.sort((a, b) => sorter(a.name, b.name)),
      emergency: emergency.value.sort((a, b) => sorter(a.name, b.name)),
      night: night.value.sort((a, b) => sorter(a.name, b.name)),
      campaign: campaign.value.sort((a, b) => sorter(a.name, b.name)),
      event: event.value.sort((a, b) => sorter(a.name, b.name)),
    };
  },
  set() {},
});

const enabled = computed({
  get() {
    return store.combatEnabled;
  },
  set(value) {
    store.setCombatEnabled(value);
  },
});
const map = computed({
  get() {
    return store.combatMap;
  },
  set(value) {
    store.setCombatMap(value);
  },
});
const draggerSlot = computed({
  get() {
    return store.combatDraggerSlot;
  },
  set(value) {
    store.setCombatDraggerSlot(value);
  },
});
const battleTimeout = computed({
  get() {
    return store.combatBattleTimeout;
  },
  set(value) {
    store.setCombatBattleTimeout(value);
  },
});
const repairThreshold = computed({
  get() {
    return store.combatRepairThreshold;
  },
  set(value) {
    store.setCombatRepairThreshold(value);
  },
});
const firstDragger = computed({
  get() {
    return store.combatDraggers[0];
  },
  set(value) {
    const draggers = store.combatDraggers;
    draggers[0] = value;
    store.setCombatDraggers(draggers);
  },
});
const secondDragger = computed({
  get() {
    return store.combatDraggers[1];
  },
  set(value) {
    const draggers = store.combatDraggers;
    draggers[1] = value;
    store.setCombatDraggers(draggers);
  },
});

const draggerSlots = ref<number[]>([1, 2, 3, 4, 5]);
</script>
