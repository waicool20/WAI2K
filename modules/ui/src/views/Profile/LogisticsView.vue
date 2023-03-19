<template>
  <h2 class="text-xl my-4 px-1">Logistics</h2>
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
      <span class="label-text">Receive mode</span>
      <select class="select w-1/4 max-w-xs" v-model="receiveMode">
        <option v-for="mode in logisticsReceiveModeList" :key="mode">
          {{ mode }}
        </option>
      </select>
    </label>
  </div>
  <h2 class="text-xl my-4 px-1">Assignments</h2>
  <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-7 gap-0.5">
    <label
      for="assignment-modal"
      class="echelon btn btn-primary btn-sm"
      v-for="(assignment, key, index) in assignments"
      :key="index"
      @click="openAssignment(key.toString())"
    >
      <span class="mr-2">Echelon #{{ key }}</span>
      <font-awesome-icon icon="fa-solid fa-arrow-up-right-from-square" />
    </label>
  </div>
  <Teleport to="body">
    <AssignmentModal
      :index="selectedAssignmentIndex"
      @close="cancelAssignment"
      @save="cancelAssignment"
    />
  </Teleport>
</template>
<script setup lang="ts">
import type { Ref } from "vue";
import { computed, ref } from "vue";
import { useProfileStore } from "@/stores/profile";
import { useClassifierStore } from "@/stores/classifiers";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import AssignmentModal from "@/components/AssignmentModal.vue";

const { logisticsReceiveModeList } = useClassifierStore();

const store = useProfileStore();

const enabled = computed({
  get() {
    return store.logisticsEnabled;
  },
  set(value) {
    store.setLogisticsEnabled(value);
  },
});

const receiveMode = computed({
  get() {
    return store.logisticsReceiveMode;
  },
  set(value) {
    store.setLogisticsReceiveMode(value);
  },
});

const assignments = computed({
  get() {
    return store.logisticsAssignments;
  },
  set(value) {
    store.setLogisticsAssignments(value);
  },
});

const selectedAssignmentIndex: Ref<string | undefined> = ref(undefined);

const openAssignment = (key: string) => {
  selectedAssignmentIndex.value = key;
};
const cancelAssignment = () => {
  selectedAssignmentIndex.value = undefined;
};
</script>
