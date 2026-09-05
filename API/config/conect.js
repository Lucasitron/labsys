
const dotenv = require('dotenv');
const path = require('path');
const { Pool } = require('pg')

const environment = process.env.NODE_ENV;
const envFile = path.resolve(__dirname, './.env', `.env.${environment}`);

dotenv.config({
    path: envFile,
    override: true
});



class Conect{
    constructor() {
        if (Conect.instance) {
            return Conect.instance;
        }

        this.pool = new Pool({
            user: process.env.USER,
            host: process.env.HOST,
            database: process.env.DB,
            password: process.env.PASSWD,
            port: process.env.PORT,
            max: process.env.MAX,
            idleTimeoutMillis: process.env.ITM
        });

        this.pool.on('connect', () => {
            console.log('Sucess: new connection established with the database');
        });

        this.pool.on('error', (err) => {
            console.log('Filed: error code:', err);
        });

        Conect.instance = this;
    }
    getPool() {
        return this.pool;
    }
    async query(text, params) {
        const client = await this.pool.connect();
        try {
            const result = await client.query(text, params);
            return result;
        } finally {
            client.release;
        }
    }

    async transaction(callback) {
        const client = await this.pool.connect();
        try {
            await client.query('BEGIN');
            const result = callback(client);
            await client.query('COMMIT');
            return result
        } catch (error) {
            await client.query('ROLLBACK');
            console.error('Erro na query:', {
                text,
                params,
                error: error.message
            });
            throw (error);
        } finally {
            client.release();
        }
    }
}

module.exports = pool;