
import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from db import Tones, ToneEnum, TonePrompts, Base


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
    # First ensure tones exist in the database
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

    # Check if prompts already exist to avoid duplicates
    existing_prompts = session.query(TonePrompts).count()
    if existing_prompts == 0:
        # Get tone IDs for reference
        caring_tone = session.query(Tones).filter(Tones.tone_name == ToneEnum.caring).first()
        neutral_tone = session.query(Tones).filter(Tones.tone_name == ToneEnum.neutral).first()
        assertive_tone = session.query(Tones).filter(Tones.tone_name == ToneEnum.assertive).first()
        encouraging_tone = session.query(Tones).filter(Tones.tone_name == ToneEnum.encouraging).first()
        
        # Define prompts for each tone
        prompts_to_insert = [
            # Caring prompts
            TonePrompts(prompt="Hey there! 👋 Just a little nudge to remember your financial goals today. You've got this!", tone_id=caring_tone.tone_id),
            TonePrompts(prompt="Thinking about your financial journey. Remember why you're saving. What's one small choice you can make today to honor your goals?", tone_id=caring_tone.tone_id),
            TonePrompts(prompt="Your financial dreams matter. Before that coffee run, pause and consider if it aligns with what you're building. We're here to support you.", tone_id=caring_tone.tone_id),
            
            # Neutral prompts
            TonePrompts(prompt="Financial Goal Alert: Your designated reminder time. Review current spending decisions against your financial objectives.", tone_id=neutral_tone.tone_id),
            TonePrompts(prompt="Daily Financial Check-in: Consider if upcoming expenses align with your financial goals. Example: Evaluate need for habitual purchases.", tone_id=neutral_tone.tone_id),
            TonePrompts(prompt="Reminder: This is your specified time for financial goal review. Unplanned spending may impact progress.", tone_id=neutral_tone.tone_id),
            
            # Assertive prompts
            TonePrompts(prompt="Stop. Before you spend, ask yourself: Is this purchase moving you closer to your financial goals? Make the conscious choice.", tone_id=assertive_tone.tone_id),
            TonePrompts(prompt="Your financial goals are not achieved by accident. Every habitual or social spend that deviates from your plan delays your progress. Choose wisely.", tone_id=assertive_tone.tone_id),
            TonePrompts(prompt="This is your reminder to act on your financial goals. Don't let a momentary desire derail your long-term success. Control your spending now.", tone_id=assertive_tone.tone_id),
            
            # Encouraging prompts
            TonePrompts(prompt="You're doing great! Keep those financial goals in mind today. Every smart choice brings you closer. Let's make it happen!", tone_id=encouraging_tone.tone_id),
            TonePrompts(prompt="Imagine reaching your financial goals! That's what every intentional decision today builds towards. You have the power to make it a reality.", tone_id=encouraging_tone.tone_id),
            TonePrompts(prompt="You're in control of your financial future! Today, choose to align your spending with your biggest dreams. You've got the strength to make incredible progress.", tone_id=encouraging_tone.tone_id),
        ]
        
        session.add_all(prompts_to_insert)
        session.commit()
        print(f"Successfully inserted {len(prompts_to_insert)} prompts.")
    else:
        print(f"Prompts already exist in database ({existing_prompts} records found).")
        
except Exception as e:
    print(f"Error inserting data: {e}")
    session.rollback()
finally:
    session.close()