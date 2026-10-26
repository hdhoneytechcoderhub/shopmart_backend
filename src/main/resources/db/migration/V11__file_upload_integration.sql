-- Phase: Product/Compliance File Upload Integration — storage keys needed to actually delete
-- files from S3/Cloudinary/local disk (the public URL alone isn't enough for that on S3/Cloudinary).
ALTER TABLE product_images ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255);
ALTER TABLE compliance_documents ADD COLUMN IF NOT EXISTS document_storage_key VARCHAR(255);
