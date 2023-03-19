import { defineStore } from "pinia";

interface LogisticsConfig {
  enabled: boolean;
  receiveMode: string;
  assignments: { [n: string]: Array<number> };
}

interface AutoBattle {
  enabled: boolean;
}

interface Dragger {
  id: string;
}

interface Combat {
  enabled: boolean;
  map: string;
  repairThreshold: number;
  battleTimeout: number;
  draggerSlot: number;
  draggers: Dragger[];
}

interface CombatReport {
  enabled: boolean;
  type: string;
}

interface CoalitionSim {
  enabled: boolean;
  preferredType: string;
}

interface CombatSim {
  enabled: boolean;
  dataSim: string;
  neuralFragment: string;
  neuralEchelon: number;
  coalition: CoalitionSim;
}

interface FactoryNode {
  enabled: boolean;
}

interface Disassembly extends FactoryNode {
  disassemble4Star: boolean;
}

interface Factory {
  enhancement: FactoryNode;
  disassembly: Disassembly;
  equipDisassembly: Disassembly;
  alwaysDisassembleAfterEnhance: boolean;
}

interface StopTime {
  enabled: boolean;
  mode: string;
  elapsedTime: string;
  specificTime: string;
}

interface StopCount {
  enabled: boolean;
  sorties: number;
}

interface StopConfig {
  enabled: boolean;
  exitProgram: boolean;
  time: StopTime;
  count: StopCount;
}

interface Profile {
  logistics: LogisticsConfig;
  auto_battle: AutoBattle;
  combat: Combat;
  combat_report: CombatReport;
  combat_simulation: CombatSim;
  factory: Factory;
  stop: StopConfig;
  name: string;
}

export const useProfileStore = defineStore("profile", {
  state: (): Profile => ({
    name: "Default",
    logistics: {
      enabled: false,
      receiveMode: "",
      assignments: {},
    },
    auto_battle: {
      enabled: false,
    },
    combat: {
      enabled: false,
      map: "",
      repairThreshold: 0,
      battleTimeout: 0,
      draggerSlot: 0,
      draggers: [],
    },
    combat_report: {
      enabled: false,
      type: "",
    },
    combat_simulation: {
      enabled: false,
      dataSim: "ADVANCED",
      neuralFragment: "ADVANCED",
      neuralEchelon: 6,
      coalition: {
        enabled: false,
        preferredType: "RANDOM",
      },
    },
    factory: {
      alwaysDisassembleAfterEnhance: false,
      enhancement: {
        enabled: false,
      },
      disassembly: {
        enabled: false,
        disassemble4Star: false,
      },
      equipDisassembly: {
        enabled: false,
        disassemble4Star: false,
      },
    },
    stop: {
      enabled: false,
      exitProgram: false,
      time: {
        enabled: false,
        mode: "",
        elapsedTime: "",
        specificTime: "",
      },
      count: {
        enabled: false,
        sorties: 0,
      },
    },
  }),
  actions: {
    async load() {
      const result = await this.axios.get(this.$api + "/profile/current");
      this.$patch(result.data);
      this.name = result.data.name_property.value;
    },
    setLogisticsEnabled(value: boolean) {
      this.logistics.enabled = value;
    },
    setLogisticsReceiveMode(value: string) {
      this.logistics.receiveMode = value;
    },
    setLogisticsAssignments(value: { [n: number]: number[] }) {
      this.logistics.assignments = value;
    },
    getAssignment(index: string) {
      return this.logistics.assignments[index];
    },
    setAssignment(index: string, assignment: number[]) {
      this.logistics.assignments[index] = assignment;
    },
    setAutoBattleEnabled(value: boolean) {
      this.auto_battle.enabled = value;
    },
    setCombatEnabled(value: boolean) {
      this.combat.enabled = value;
    },
    setCombatMap(map: string) {
      this.combat.map = map;
    },
    setCombatRepairThreshold(value: number) {
      this.combat.repairThreshold = value;
    },
    setCombatBattleTimeout(value: number) {
      this.combat.battleTimeout = value;
    },
    setCombatDraggerSlot(value: number) {
      this.combat.draggerSlot = value;
    },
    setCombatDraggers(draggers: Dragger[]) {
      this.combat.draggers = draggers;
    },
    swapCombatDraggers() {
      this.combat.draggers = this.combat.draggers.reverse();
    },
    setCombatReportEnabled(value: boolean) {
      this.combat_report.enabled = value;
    },
    setCombatReportType(value: string) {
      this.combat_report.type = value;
    },
    setCombatSimDataEnabled(value: boolean) {
      this.combat_simulation.enabled = value;
    },
    setCombatSimDataType(value: string) {
      this.combat_simulation.dataSim = value;
    },
    setCombatSimNeuralType(value: string) {
      this.combat_simulation.neuralFragment = value;
    },
    setCombatSimNeuralEchelon(value: number) {
      this.combat_simulation.neuralEchelon = value;
    },
    setCombatSimCoalitionEnabled(value: boolean) {
      this.combat_simulation.coalition.enabled = value;
    },
    setCombatSimCoalitionPreferredType(value: string) {
      this.combat_simulation.coalition.preferredType = value;
    },
    setFactoryEnhancementEnabled(value: boolean) {
      this.factory.enhancement.enabled = value;
    },
    setFactoryDisassemblyEnabled(value: boolean) {
      this.factory.disassembly.enabled = value;
    },
    setFactoryDisassembly4StarEnabled(value: boolean) {
      this.factory.disassembly.disassemble4Star = value;
    },
    setFactoryEquipmentDisassemblyEnabled(value: boolean) {
      this.factory.equipDisassembly.enabled = value;
    },
    setFactoryEquipmentDisassembly4StarEnabled(value: boolean) {
      this.factory.equipDisassembly.disassemble4Star = value;
    },
    setFactoryAlwaysDisassembleAfterEnhance(value: boolean) {
      this.factory.alwaysDisassembleAfterEnhance = value;
    },
    setStopEnabled(value: boolean) {
      this.stop.enabled = value;
    },
    setStopExitProgram(value: boolean) {
      this.stop.exitProgram = value;
    },
    setStopTimeEnabled(value: boolean) {
      this.stop.time.enabled = value;
    },
    setStopTimeMode(value: string) {
      this.stop.time.mode = value;
    },
    setStopTimeElapsedTime(value: string) {
      this.stop.time.elapsedTime = value;
    },
    setStopTimeSpecificTime(value: string) {
      this.stop.time.specificTime = value;
    },
    setStopCountEnabled(value: boolean) {
      this.stop.count.enabled = value;
    },
    setStopCountSorties(value: number) {
      this.stop.count.sorties = value;
    },
  },
  getters: {
    logisticsEnabled: (state): boolean => {
      return state.logistics.enabled;
    },
    logisticsAssignments: (state): { [n: string]: Array<number> } => {
      return state.logistics.assignments;
    },
    logisticsReceiveMode: (state): string => {
      return state.logistics.receiveMode;
    },
    autoBattleEnabled: (state): boolean => {
      return state.auto_battle.enabled;
    },
    combatEnabled: (state): boolean => {
      return state.combat.enabled;
    },
    combatMap: (state): string => {
      return state.combat.map;
    },
    combatRepairThreshold: (state): number => {
      return state.combat.repairThreshold;
    },
    combatBattleTimeout: (state): number => {
      return state.combat.battleTimeout;
    },
    combatDraggerSlot: (state): number => {
      return state.combat.draggerSlot;
    },
    combatDraggers: (state): Dragger[] => {
      return state.combat.draggers;
    },
    combatReportEnabled: (state): boolean => {
      return state.combat_report.enabled;
    },
    combatReportType: (state): string => {
      return state.combat_report.type;
    },
    combatSimDataEnabled: (state): boolean => {
      return state.combat_simulation.enabled;
    },
    combatSimDataType: (state): string => {
      return state.combat_simulation.dataSim;
    },
    combatSimNeuralType: (state): string => {
      return state.combat_simulation.neuralFragment;
    },
    combatSimNeuralEchelon: (state): number => {
      return state.combat_simulation.neuralEchelon;
    },
    combatSimCoalitionEnabled: (state): boolean => {
      return state.combat_simulation.coalition.enabled;
    },
    combatSimCoalitionPreferredTYpe: (state): string => {
      return state.combat_simulation.coalition.preferredType;
    },
    factoryEnhancementEnabled: (state): boolean => {
      return state.factory.enhancement.enabled;
    },
    factoryDisassemblyEnabled: (state): boolean => {
      return state.factory.disassembly.enabled;
    },
    factoryDisassembly4StarEnabled: (state): boolean => {
      return state.factory.disassembly.disassemble4Star;
    },
    factoryEquipmentDisassemblyEnabled: (state): boolean => {
      return state.factory.equipDisassembly.enabled;
    },
    factoryEquipmentDisassembly4StarEnabled: (state): boolean => {
      return state.factory.equipDisassembly.disassemble4Star;
    },
    factoryAlwaysDisassembleAfterEnhance: (state): boolean => {
      return state.factory.alwaysDisassembleAfterEnhance;
    },
    stopEnabled: (state): boolean => {
      return state.stop.enabled;
    },
    stopExitProgram: (state): boolean => {
      return state.stop.exitProgram;
    },
    stopTimeEnabled: (state): boolean => {
      return state.stop.time.enabled;
    },
    stopTimeMode: (state): string => {
      return state.stop.time.mode;
    },
    stopTimeElapsedTime: (state): string => {
      return state.stop.time.elapsedTime;
    },
    stopTimeSpecificTime: (state): string => {
      return state.stop.time.specificTime;
    },
    stopCountEnabled: (state): boolean => {
      return state.stop.count.enabled;
    },
    stopCountSorties: (state): number => {
      return state.stop.count.sorties;
    },
  },
});
