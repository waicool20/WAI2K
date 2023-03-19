<template>
  <div class="basis-full shadow-xl mb-8 p-8 bg-neutral rounded-md grid grid-cols-12 gap-4">
    <h2 class="w-full mb-2 text-3xl col-span-12">Stats</h2>
    <div class="stats shadow col-span-4">
      <div class="stat">
        <div class="stat-title">Logistics sent</div>
        <div class="stat-value">89</div>
      </div>
      <div class="stat">
        <div class="stat-title">Logistics received</div>
        <div class="stat-value">92</div>
      </div>
    </div>
    <div class="stats shadow col-span-4">
      <div class="stat">
        <div class="stat-title">Sorties done</div>
        <div class="stat-value">59</div>
      </div>
      <div class="stat">
        <div class="stat-title">Sorties per hour (SPH)</div>
        <div class="stat-value">12.5</div>
      </div>
    </div>
    <div class="stats shadow col-span-4">
      <div class="stat">
        <div class="stat-title">Repairs</div>
        <div class="stat-value">0</div>
      </div>
      <div class="stat">
        <div class="stat-title">Sorties per repair (SPR)</div>
        <div class="stat-value">
          <font-awesome-icon icon="fa-solid fa-infinity"/>
        </div>
      </div>
    </div>
    <div class="stats shadow col-span-4">
      <div class="stat">
        <div class="stat-title">Enhancements done</div>
        <div class="stat-value">10</div>
      </div>
      <div class="stat">
        <div class="stat-title">Dolls used</div>
        <div class="stat-value">254</div>
      </div>
    </div>
    <div class="stats shadow col-span-4">
      <div class="stat">
        <div class="stat-title">Disassembles done</div>
        <div class="stat-value">0</div>
      </div>
      <div class="stat">
        <div class="stat-title">Dolls used</div>
        <div class="stat-value">0</div>
      </div>
    </div>
    <div class="stats shadow col-span-4">
      <div class="stat w-1/2">
        <div class="stat-title">Equipment disassembles done</div>
        <div class="stat-value">0</div>
      </div>
      <div class="stat w-1/2">
        <div class="stat-title">Equipment used</div>
        <div class="stat-value">0</div>
      </div>
    </div>
    <div class="stats shadow col-span-12">
      <div class="stat w-1/4">
        <div class="stat-title">Combat reports written</div>
        <div class="stat-value">0</div>
      </div>
      <div class="stat w-1/4">
        <div class="stat-title">Simulation energy used</div>
        <div class="stat-value">0</div>
      </div>
      <div class="stat w-1/4">
        <div class="stat-title">Coalition energy used</div>
        <div class="stat-value">0</div>
      </div>
      <div class="stat w-1/4">
        <div class="stat-title">Game restarts</div>
        <div class="stat-value">0</div>
      </div>
    </div>
    <h2 class="w-full my-2 text-3xl col-span-12">Logistics</h2>
    <div class="stats shadow col-span-12">
      <div class="stat w-1/4">
        <div class="stat-title">10-4</div>
        <div class="stat-value my-3 text-md">
          <div class="radial-progress text-xs mr-4" style="--value:100;">
            <div class="avatar h-14 w-14 p-1">
              <img src="@/assets/wa2000.gif" alt="WAI2K"/>
            </div>
          </div>
          <span>Echelon 10</span>
        </div>
        <div class="stat-desc">00:00:00</div>
      </div>
      <div class="stat w-1/4">
        <div class="stat-title">10-4</div>
        <div class="stat-value my-3 text-md">
          <div class="radial-progress text-xs mr-4" style="--value:100;">
            <div class="avatar h-14 w-14 p-1">
              <img src="@/assets/wa2000.gif" alt="WAI2K"/>
            </div>
          </div>
          <span>Echelon 10</span>
        </div>
        <div class="stat-desc">00:00:00</div>
      </div>
      <div class="stat w-1/4">
        <div class="stat-title">10-4</div>
        <div class="stat-value my-3 text-md">
          <div class="radial-progress text-xs mr-4" style="--value:100;">
            <div class="avatar h-14 w-14 p-1">
              <img src="@/assets/wa2000.gif" alt="WAI2K"/>
            </div>
          </div>
          <span>Echelon 10</span>
        </div>
        <div class="stat-desc">00:00:00</div>
      </div>
      <div class="stat w-1/4">
        <div class="stat-title">10-4</div>
        <div class="stat-value my-3 text-md">
          <div class="radial-progress text-xs mr-4" style="--value:100;">
            <div class="avatar h-14 w-14 p-1">
              <img src="@/assets/wa2000.gif" alt="WAI2K"/>
            </div>
          </div>
          <span>Echelon 10</span>
        </div>
        <div class="stat-desc">00:00:00</div>
      </div>
    </div>
  </div>
</template>
<script>
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

export default {
  components: {FontAwesomeIcon}
}
</script>