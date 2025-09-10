import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from db import Tones, ToneEnum, Base


DB_USER = os.getenv("POSTGRES_USER", "postgres")
DB_PASSWORD = os.getenv("POSTGRES_PASSWORD", "0351036") 
DB_HOST = os.getenv("POSTGRES_HOST", "localhost")
DB_PORT = os.getenv("POSTGRES_PORT", "5432")
DB_NAME = os.getenv("POSTGRES_DB", "Nudger")

DATABASE_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
engine = create_engine(DATABASE_URL, echo=False)

# Create tables if they don't exist
Base.metadata.create_all(engine)

# Use ORM session instead of raw connection
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
session = SessionLocal()

try:
    # Check if tones already exist to avoid duplicates
    existing_tones = session.query(Tones).count()
    if existing_tones == 0:
        tones_to_insert = [
            Tones(tone_name=ToneEnum.caring),
            Tones(tone_name=ToneEnum.neutral),
            Tones(tone_name=ToneEnum.assertive),
            Tones(tone_name=ToneEnum.encouraging),
        ]
        
        session.add_all(tones_to_insert)
        session.commit()
        print(f"Successfully inserted {len(tones_to_insert)} tones.")
    else:
        print(f"Tones already exist in database ({existing_tones} records found).")
        
except Exception as e:
    print(f"Error inserting tones: {e}")
    session.rollback()
finally:
    session.close()