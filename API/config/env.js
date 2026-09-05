const dotenv = require('dotenv');

function environmentsSelec(ambiente) {
    const env = process.env.NODE_ENV || ambiente;
    switch (env) {
        case 'development':
            dotenv.config({ path: '.env.development' });
            break;
        case 'test':
            dotenv.config({ path: '.env.test' });
            break;
        case 'staging':
            dotenv.config({ path: '.env.staging' });
            break;
        case 'production':
            dotenv.config({ path: '.env.production' });
            break;
        default:
            throw new Error(`Unknown environment: ${env}`);
    }
    return env;
}

module.exports = { environmentsSelec };
