<template>
  <main>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Restart enabled</span>
        <input type="checkbox" class="toggle" checked />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Average delay (ms)</span>
        <input
          type="number"
          placeholder="delay (ms)"
          step="1"
          min="1"
          max="9999"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Delay coefficient threshold</span>
        <input
          type="number"
          step="0.1"
          min="0.1"
          max="10"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Max restarts</span>
        <input
          type="number"
          placeholder="restarts"
          step="1"
          min="-1"
          max="9999"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
  </main>
</template>
