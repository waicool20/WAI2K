import {defineStore} from "pinia";
import {inject, ref} from "vue";
import type {AxiosStatic} from "axios";

interface Doll {
  name: string;
  stars: number;
  type: string;
  id: string;
}

interface Map {
  name: string;
  type?: string;
  chapter?: number;
  number?: number;
}

interface LogisticMap {
  formattedString: string;
  chapter: number;
  chapterIndex: number;
  number: number;
}

interface MapClassifier {
  normal: Map[];
  emergency: Map[];
  night: Map[];
  campaign: Map[];
  event: Map[];
  logistics?: LogisticMap[];
}

export const useClassifierStore = defineStore("classifiers", () => {
  const axios: AxiosStatic = <AxiosStatic>inject("axios");
  const $api = inject("$api");

  const logisticsReceiveModeList = ref<String[]>([]);
  const combatReportTypeList = ref<String[]>([]);
  const captureMethodList = ref<String[]>([]);
  const compressionModeList = ref<String[]>([]);
  const dolls = ref<Doll[]>([]);
  const combatSimData = ref<String[]>([]);
  const combatSimNeural = ref<String[]>([]);
  const combatSimCoalition = ref<String[]>([]);
  const timeStopType = ref<String[]>([]);
  const maps = ref<MapClassifier>({
    campaign: [],
    emergency: [],
    event: [],
    logistics: [],
    night: [],
    normal: [],
  });

  const load = async () => {
    const [result, mapsResult] = await Promise.all([
      axios.get($api + "/classifier"),
      axios.get($api + "/classifier/maps"),
    ]);

    logisticsReceiveModeList.value = result.data.logisticsReceiveModeList;
    combatReportTypeList.value = result.data.combatReportTypeList;
    captureMethodList.value = result.data.captureMethodList;
    compressionModeList.value = result.data.compressionModeList;
    dolls.value = result.data.dolls;
    combatSimData.value = result.data.combatSimData;
    combatSimNeural.value = result.data.combatSimNeural;
    combatSimCoalition.value = result.data.combatSimCoalition;
    timeStopType.value = result.data.timeStopType;
    maps.value = mapsResult.data;
    console.log(result, maps);
  };
  return {
    logisticsReceiveModeList,
    combatReportTypeList,
    captureMethodList,
    compressionModeList,
    dolls,
    combatSimData,
    combatSimNeural,
    combatSimCoalition,
    timeStopType,
    maps,
    load,
  };
});
