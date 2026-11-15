// Tiny IndexedDB wrapper. Stores: kv (child profile, settings), vaccines, visits.
const DB_NAME = 'child-health';
const DB_VERSION = 1;
let dbPromise;

function open() {
  if (!dbPromise) {
    dbPromise = new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = () => {
        const db = req.result;
        db.createObjectStore('kv');
        db.createObjectStore('vaccines', { keyPath: 'id', autoIncrement: true });
        db.createObjectStore('visits', { keyPath: 'id', autoIncrement: true });
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }
  return dbPromise;
}

async function run(store, mode, fn) {
  const db = await open();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(store, mode);
    const result = fn(tx.objectStore(store));
    tx.oncomplete = () => resolve(result && 'result' in result ? result.result : result);
    tx.onerror = () => reject(tx.error);
    tx.onabort = () => reject(tx.error);
  });
}

export const db = {
  getAll: (store) => run(store, 'readonly', (s) => s.getAll()),
  put: (store, value) => run(store, 'readwrite', (s) => s.put(value)),
  putMany: (store, values) => run(store, 'readwrite', (s) => { values.forEach((v) => s.put(v)); }),
  delete: (store, key) => run(store, 'readwrite', (s) => s.delete(key)),
  clear: (store) => run(store, 'readwrite', (s) => s.clear()),
  kvGet: (key) => run('kv', 'readonly', (s) => s.get(key)),
  kvSet: (key, value) => run('kv', 'readwrite', (s) => s.put(value, key)),
};
